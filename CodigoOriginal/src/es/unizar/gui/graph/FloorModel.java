package es.unizar.gui.graph;

import java.util.Map;

public interface FloorModel {
    int getRoomFromPosition(int x, int y);
    int getRoomCount();
    String getItemLocation(long itemId);
    int getRoomLabel(int roomIndex);
    Map<Long, String> getItemLocationDictionary();
    String getGraphItemRoom(int itemId);
}

