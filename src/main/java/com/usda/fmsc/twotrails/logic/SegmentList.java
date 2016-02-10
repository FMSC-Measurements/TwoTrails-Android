package com.usda.fmsc.twotrails.logic;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SegmentList {
    private List<Segment> list;
    private boolean sorted;

    public SegmentList() {
        list = new ArrayList<>();
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
            Collections.sort(list, segmentComparator);

            sorted = true;
        }

        Segment current = list.get(0);
        list.remove(0);

        return current;
    }

    public boolean hasNext() {
        return list.size() > 0;
    }


    private static Comparator<Segment> segmentComparator = new Comparator<Segment>() {
        @Override
        public int compare(Segment a, Segment b) {
            if (a == null) {
                if (b == null)
                    return 0;
                return 1;
            } else if (b == null) {
                return -1;
            }

            if (a.getWeight() == b.getWeight())
                return 0;
            if (a.getWeight() > b.getWeight())
                return -1;
            else
                return 1;
        }
    };
}
