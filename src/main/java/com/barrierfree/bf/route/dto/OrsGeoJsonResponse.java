package com.barrierfree.bf.route.dto;

import java.util.List;

public record OrsGeoJsonResponse(List<Feature> features) {

  public record Feature(Geometry geometry, Properties properties) {}

  public record Geometry(List<List<Double>> coordinates) {}

  public record Properties(Summary summary, Extras extras) {}

  public record Extras(SurfaceInfo surface) {}

  public record SurfaceInfo(List<List<Integer>> values) {}

  public record Summary(double distance, double duration) {}
}
