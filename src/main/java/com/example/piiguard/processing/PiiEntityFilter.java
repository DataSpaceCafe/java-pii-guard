package com.example.piiguard.processing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Filters overlapping entities and sorts by start offset.
 */
public final class PiiEntityFilter {
  private PiiEntityFilter() {
  }

  /**
   * Removes overlapping entities by keeping higher score spans.
   *
   * @param entities entities to filter
   * @return filtered list
   */
  public static List<PiiEntity> filterOverlaps(List<PiiEntity> entities) {
    if (entities == null || entities.isEmpty()) {
      return List.of();
    }
    var sorted = new ArrayList<>(entities);
    sorted.sort(Comparator.comparingInt((PiiEntity e) -> e.start).thenComparingDouble(e -> -e.score));

    var result = new ArrayList<PiiEntity>();
    var currentEnd = -1;
    for (var entity : sorted) {
      if (entity.start >= currentEnd) {
        result.add(entity);
        currentEnd = entity.end;
      }
    }
    return result;
  }
}
