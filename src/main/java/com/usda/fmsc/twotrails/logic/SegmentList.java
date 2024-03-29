package com.usda.fmsc.twotrails.logic;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SegmentList {
    private final List<Segment> list = new ArrayList<>();
    private boolean sorted;

    public SegmentList() {
        sorted = false;
    }

    public void addSegment(Segment segment) {
        list.add(segment);
        sorted = false;
    }

    public Segment next() {
        if (list.size() == 0)
            return null;

        if (!sorted && list.size() != 1) {
            list.sort(segmentComparator);

            sorted = true;
        }

        Segment current = list.get(0);
        list.remove(0);

        return current;
    }

    public boolean hasNext() {
        return list.size() > 0;
    }


    private final static Comparator<Segment> segmentComparator = (a, b) -> {
        if (a == null) {
            if (b == null)
                return 0;
            return 1;
        } else if (b == null) {
            return -1;
        }

        return Integer.compare(b.getWeight(), a.getWeight());
    };
}
