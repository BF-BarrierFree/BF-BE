package com.barrierfree.bf.taxi.batch;

import com.barrierfree.bf.taxi.dto.TagoTaxiCenterResponse;
import com.barrierfree.bf.taxi.entity.TaxiCenter;
import com.barrierfree.bf.taxi.repository.TaxiCenterRepository;
import com.barrierfree.bf.taxi.service.OpenRouterService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
// 삭제: import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaxiCenterBatchService {

    private final TaxiCenterRepository taxiCenterRepository;
    private final WebClient tagoWebClient;
    private final OpenRouterService openRouterService;

    @Value("${tago.api.taxi-center-base-url}")
    private String taxiCenterBaseUrl;

    @Value("${tago.api.service-key}")
    private String serviceKey;

    @Scheduled(cron = "0 0 3 * * SUN")
    // @Transactional <-- [핵심 수정 1] 이 어노테이션을 반드시 지워주세요! (네트워크 통신과 DB 트랜잭션을 분리)
    public void fetchAndUpsertTaxiCenters() {
        log.info("[Batch] 교통약자이동지원센터 공공데이터 갱신 배치를 시작합니다.");

        int pageNo = 1;
        int numOfRows = 100;
        boolean hasNext = true;
        int upsertCount = 0;

        ObjectMapper objectMapper = new ObjectMapper()
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        while (hasNext) {
            try {
                final int currentPageNo = pageNo; 

                // fromHttpUrl 대신 fromUriString 사용으로 컴파일 에러 해결
                String requestUri = UriComponentsBuilder.fromUriString(taxiCenterBaseUrl)
                        .path("/center_info_v2")
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("pageNo", currentPageNo)
                        .queryParam("numOfRows", numOfRows)
                        .queryParam("type", "json")
                        .build(false)
                        .toUriString();

                String jsonResponse = tagoWebClient.get()
                        .uri(requestUri)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                if (jsonResponse == null || jsonResponse.isBlank()) {
                    break;
                }

                JsonNode rootNode = objectMapper.readTree(jsonResponse);
                JsonNode bodyNode = rootNode.has("response") ? 
                        rootNode.get("response").get("body") : rootNode.get("body");

                if (bodyNode == null || !bodyNode.has("item")) {
                    break;
                }

                TagoTaxiCenterResponse.Body body = objectMapper.treeToValue(bodyNode, TagoTaxiCenterResponse.Body.class);
                if (body.getItem() == null || body.getItem().isEmpty()) {
                    break;
                }

                // 1. N+1 문제 해결: 페이지 내 전체 Center ID 추출 및 DB In 쿼리 조회
                List<String> centerIds = body.getItem().stream()
                        .map(TagoTaxiCenterResponse.CenterItem::getCntrId)
                        .filter(Objects::nonNull)
                        .toList();
                
                List<TaxiCenter> existingCentersList = taxiCenterRepository.findByCenterIdIn(centerIds);
                Map<String, TaxiCenter> existingCenterMap = existingCentersList.stream()
                        .collect(Collectors.toMap(TaxiCenter::getCenterId, c -> c));

                // 2. 개별 아이템 처리 및 즉시 저장
                for (TagoTaxiCenterResponse.CenterItem item : body.getItem()) {
                    try {
                        TaxiCenter existingCenter = existingCenterMap.get(item.getCntrId());
                        TaxiCenter processedCenter = processTaxiCenter(item, existingCenter);

                        if (processedCenter != null) {
                            taxiCenterRepository.save(processedCenter);
                            upsertCount++;
                        }
                    } catch (Exception e) {
                        log.error("[Batch] Center ID: {} 처리 실패. 다음 센터로 진행합니다: {}",
                                item.getCntrId(), e.getMessage());
                    }
                }

                if (body.getItem().size() < numOfRows) {
                    hasNext = false;
                } else {
                    pageNo++;
                }

            } catch (Exception e) {
                log.error("[Batch] 센터 데이터 적재 중 오류 발생 (pageNo: {}): {}", pageNo, e.getMessage(), e);
                break;
            }
        }

        log.info("[Batch] 교통약자이동지원센터 갱신 배치가 완료되었습니다. 총 {} 건 처리됨.", upsertCount);
    }

    private TaxiCenter processTaxiCenter(TagoTaxiCenterResponse.CenterItem item, TaxiCenter existingCenter) {
        if (item.getCntrId() == null) return null;

        Double latitude = parseDouble(item.getLat());
        Double longitude = parseDouble(item.getLot());

        if (latitude == null || longitude == null) {
            log.warn("[Batch] 유효하지 않은 좌표입니다. 스킵합니다. Center ID: {}", item.getCntrId());
            return null;
        }

        // 규정 변경 여부 확인 (신규 센터이거나 기준일자가 달라진 경우)
        boolean requiresNewSchema = existingCenter == null || 
            (existingCenter.getReferenceDate() == null || !existingCenter.getReferenceDate().equals(item.getTotCrtrYmd()))
            || !openRouterService.isValidFormSchema(existingCenter.getFormSchema());

        String formSchema = null;
        if (requiresNewSchema) {
            log.info("[Batch] Center ID: {} 의 LLM 폼 스키마 생성 중...", item.getCntrId());
            try {
                formSchema = openRouterService.generateFormSchema(item.getUtztnTrgtExpln(), item.getRsvtGdMttr());
            } catch (Exception e) {
                log.warn("[Batch] Center ID: {} 폼 스키마 생성 실패. 기본 빈 스키마로 적재합니다: {}",
                        item.getCntrId(), e.getMessage());
                formSchema = "[]";
            }

        } else {
            formSchema = existingCenter.getFormSchema();
        }

        TaxiCenter newData = TaxiCenter.builder()
                .centerId(item.getCntrId())
                .centerName(item.getCntrNm())
                .localGovName(item.getLclgvNm())
                .roadAddress(item.getCntrRoadNmAddr())
                .latitude(latitude)
                .longitude(longitude)
                .phoneNumber(item.getCntrTelno())
                .reservationUrl(item.getRsvtSiteUrlAddr())
                .appName(item.getAppSrvcNm())
                .weekdayReservationStartTime(item.getWkdyRsvtBgngTm())
                .weekdayReservationEndTime(item.getWkdyRsvtEndTm())
                .weekdayOperationStartTime(item.getWkdyOprBgngTm())
                .weekdayOperationEndTime(item.getWkdyOprEndTm())
                .weekendOperationYn(item.getWkndOperYn())
                .weekendOperationExplanation(item.getWkndOperHrExpln())
                .withinRegionName(item.getWtjrOprRgnNm())
                .outsideRegionName(item.getBtjrOprRgnNm())
                .targetExplanation(item.getUtztnTrgtExpln())
                .dailyUsageLimit(item.getDayVhclUtztnNmtm())
                .advanceReservationExplanation(item.getBfhdRsvtPrdExpln())
                .basicChargeExplanation(item.getBscCrgExpln())
                .extraChargeExplanation(item.getExchrgCrgExpln())
                .reservationNotice(item.getRsvtGdMttr())
                .referenceDate(item.getTotCrtrYmd())
                .formSchema(formSchema)
                .build();

        if (existingCenter != null) {
            existingCenter.updateInfo(newData);
            return existingCenter;
        } else {
            return newData;
        }
    }

    private Double parseDouble(String value) {
        try {
            return (value != null && !value.isBlank()) ? Double.parseDouble(value) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}