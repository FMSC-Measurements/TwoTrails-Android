package com.usda.fmsc.twotrails.objects.media;

import org.joda.time.DateTime;

import java.util.ArrayList;

//TODO implement PositionTimeline Parcelable
public class PositionTimeline {
    private ArrayList<FramePosition> _Timeline;


    public PositionTimeline() {
        this(new ArrayList<FramePosition>());
    }

    public PositionTimeline(ArrayList<FramePosition> positions) {
        _Timeline = positions;
    }

    public PositionTimeline(PositionTimeline timeline) {
        _Timeline = new ArrayList<>(timeline._Timeline);
    }


    FramePosition get(int position) {
        return _Timeline.get(position);
    }

    FramePosition getPosition(int millisElapsed) throws OutOfTimelineException {
        DateTime time = new DateTime(getStartingPosition().getTime());
        time.plusMillis(millisElapsed);
        return getPosition(time);
    }

    FramePosition getPosition(DateTime time) throws OutOfTimelineException {
        if (getStartingPosition().getTime().isAfter(time) || getEndingPosition().getTime().isBefore(time))
            throw new OutOfTimelineException();

        FramePosition current, lastTime = getStartingPosition();

        if (lastTime.getTime().equals(time))
            return lastTime;

        if (getEndingPosition().getTime().equals(time))
            return getEndingPosition();

        for (int i = 1; i < _Timeline.size(); i++) {
            current = _Timeline.get(i);
            if (lastTime.getTime().isBefore(time) && current.getTime().isAfter(time))
                break;

            lastTime = current;
        }

        return lastTime;
    }

    FramePosition getStartingPosition() {
        return _Timeline.get(0);
    }

    FramePosition getEndingPosition() {
        return _Timeline.get(_Timeline.size() - 1);
    }

    void addPosition(FramePosition position) {
        _Timeline.add(position);
    }


    public class FramePosition implements IOrientation {
        private DateTime _Time;
        private Double _Azimuth;
        private Double _Pitch;
        private Double _Roll;


        public FramePosition(Double az, Double pitch, Double roll) {
            this(az, pitch, roll, DateTime.now());
        }

        public FramePosition(Double az, Double pitch, Double roll, DateTime time) {
            _Azimuth = az;
            _Pitch = pitch;
            _Roll = roll;
            _Time = time;
        }


        public DateTime getTime() {
            return _Time;
        }

        public void setTime(DateTime time) {
            _Time = time;
        }

        @Override
        public Double getAzimuth() {
            return _Azimuth;
        }

        public void setAzimuth(Double azimuth) {
            _Azimuth = azimuth;
        }

        @Override
        public Double getPitch() {
            return _Pitch;
        }

        public void setPitch(Double pitch) {
            _Pitch = pitch;
        }

        @Override
        public Double getRoll() {
            return _Roll;
        }

        public void setRoll(Double roll) {
            _Roll = roll;
        }
    }


    public class OutOfTimelineException extends Exception {
        public OutOfTimelineException() {
            super();
        }

        public OutOfTimelineException(String message) {
            super(message);
        }
    }
}
