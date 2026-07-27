package com.barrierfree.bf.route.dto;

public record RouteSurfaceSegment(
    int startIndex, int endIndex, Integer surfaceCode, String surfaceLabel) {}
