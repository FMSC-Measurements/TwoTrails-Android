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


    public class FramePosition implements TtOrientation {
        private DateTime _Time;
        private Float _Azimuth;
        private Float _Pitch;
        private Float _Roll;


        public FramePosition(Float az, Float pitch, Float roll) {
            this(az, pitch, roll, DateTime.now());
        }

        public FramePosition(Float az, Float pitch, Float roll, DateTime time) {
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
        public Float getAzimuth() {
            return _Azimuth;
        }

        public void setAzimuth(Float azimuth) {
            _Azimuth = azimuth;
        }

        @Override
        public Float getPitch() {
            return _Pitch;
        }

        public void setPitch(Float pitch) {
            _Pitch = pitch;
        }

        @Override
        public Float getRoll() {
            return _Roll;
        }

        public void setRoll(Float roll) {
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
