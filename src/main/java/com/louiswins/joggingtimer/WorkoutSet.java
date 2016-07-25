package com.louiswins.joggingtimer;

import android.content.Context;

import org.jetbrains.annotations.NotNull;

public class WorkoutSet {
    @NotNull
    private final Workout[] workouts;

    public WorkoutSet(@NotNull Workout[] workouts) {
        this.workouts = workouts;
    }

    public @NotNull Workout get(int i) {
        if (i < 0 || i >= length()) {
            throw new IndexOutOfBoundsException();
        }
        return workouts[i];
    }
    public int length() {
        return workouts.length;
    }

    public static class Workout {
        @NotNull
        private final String title;
        @NotNull
        private final String description;
        @NotNull
        private final Segment[] segments;
        @NotNull
        private final long[] subLengths;
        private final long totalLength;

        private Workout(@NotNull String title, @NotNull String description, @NotNull Segment[] segments) {
            this.title = title;
            this.description = description;
            this.segments = segments;
            subLengths = new long[segments.length];
            subLengths[0] = 0;
            for (int i = 1; i < segments.length; ++i) {
                this.subLengths[i] = this.subLengths[i - 1] + segments[i - 1].length();
            }
            totalLength = subLengths[subLengths.length - 1] + segments[segments.length - 1].length();
        }

        public @NotNull String getTitle() {
            return title;
        }

        public @NotNull String getDescription() {
            return description;
        }

        public int numberSegments() {
            return segments.length;
        }

        public @NotNull Segment getSegment(int i) {
            if (i < 0 || i >= numberSegments()) {
                throw new IndexOutOfBoundsException();
            }
            return segments[i];
        }

        public long getLengthUpTo(int i) {
            if (i == numberSegments()) {
                return totalLength;
            }
            if (i < 0 || i > numberSegments()) {
                throw new IndexOutOfBoundsException();
            }
            return subLengths[i];
        }

        public long getLength() {
            return totalLength;
        }
    }

    public enum Type {
        WARMUP,
        WALK,
        JOG;

        public @NotNull String toString(Context ctx) {
            final int resId;
            switch(this) {
                case WARMUP: resId = R.string.warmup_label; break;
                case WALK: resId = R.string.walk_label; break;
                case JOG: resId = R.string.jog_label; break;
                default: throw new IllegalStateException("Segment type is not valid");
            }
            return ctx.getString(resId);
        }
    }

    public static class Segment {
        private Type type;
        private long lengthInMillis; // in ms

        public Segment(Type type, long lengthInMillis) {
            this.type = type;
            this.lengthInMillis = lengthInMillis;
        }

        public Type getType() { return type; }
        public long length() { return lengthInMillis; }
    }



    private static WorkoutSet c25kWorkouts;
    public static synchronized @NotNull WorkoutSet loadC25kWorkouts(Context ctx) {
        if (c25kWorkouts != null) {
            return c25kWorkouts;
        }
        String[] workoutTitles = ctx.getResources().getStringArray(R.array.c25k_titles);
        String[] workoutDescriptions = ctx.getResources().getStringArray(R.array.c25k_descriptions);
        Workout[] workouts = new Workout[workoutTitles.length];
        for (int i = 0; i < workouts.length; ++i) {
            workouts[i] = new Workout(workoutTitles[i], workoutDescriptions[i], c25kWorkoutSegmentData[i]);
        }
        return c25kWorkouts = new WorkoutSet(workouts);
    }


    private static Segment S(Type type, long lengthInSecs) {
        return new Segment(type, lengthInSecs * 1000);
    }
    private static final Segment[][] c25kWorkoutSegmentData;
    static {
        Segment[] testWorkout = { S(Type.WARMUP, 10), S(Type.JOG, 8), S(Type.WALK, 8), S(Type.JOG, 8) };
        Segment[] week1 = { S(Type.WARMUP, 300), S(Type.JOG, 60), S(Type.WALK, 90), S(Type.JOG, 60),
                S(Type.WALK, 90), S(Type.JOG, 60), S(Type.WALK, 90), S(Type.JOG, 60), S(Type.WALK, 90),
                S(Type.JOG, 60), S(Type.WALK, 90), S(Type.JOG, 60), S(Type.WALK, 90), S(Type.JOG, 60),
                S(Type.WALK, 90), S(Type.JOG, 60), S(Type.WALK, 90) };
        Segment[] week2 = { S(Type.WARMUP, 300), S(Type.JOG, 90), S(Type.WALK, 120), S(Type.JOG, 90),
                S(Type.WALK, 120), S(Type.JOG, 90), S(Type.WALK, 120), S(Type.JOG, 90), S(Type.WALK, 120),
                S(Type.JOG, 90), S(Type.WALK, 120), S(Type.JOG, 90), S(Type.WALK, 60) };
        Segment[] week3 = { S(Type.WARMUP, 300), S(Type.JOG, 90), S(Type.WALK, 90), S(Type.JOG, 180),
                S(Type.WALK, 180), S(Type.JOG, 90), S(Type.WALK, 90), S(Type.JOG, 180), S(Type.WALK, 180) };
        Segment[] week4 = { S(Type.WARMUP, 300), S(Type.JOG, 180), S(Type.WALK, 90), S(Type.JOG, 300),
                S(Type.WALK, 150), S(Type.JOG, 180), S(Type.WALK, 90), S(Type.JOG, 300) };
        Segment[] w5d1  = { S(Type.WARMUP, 300), S(Type.JOG, 300), S(Type.WALK, 180), S(Type.JOG, 300),
                S(Type.WALK, 180), S(Type.JOG, 300) };
        Segment[] w5d2  = { S(Type.WARMUP, 300), S(Type.JOG, 480), S(Type.WALK, 300), S(Type.JOG, 480) };
        Segment[] w5d3  = { S(Type.WARMUP, 300), S(Type.JOG, 1200) };
        Segment[] w6d1  = { S(Type.WARMUP, 300), S(Type.JOG, 300), S(Type.WALK, 180), S(Type.JOG, 480),
                S(Type.WALK, 180), S(Type.JOG, 300) };
        Segment[] w6d2  = { S(Type.WARMUP, 300), S(Type.JOG, 600), S(Type.WALK, 180), S(Type.JOG, 600) };
        Segment[] w6d3  = { S(Type.WARMUP, 300), S(Type.JOG, 1320) };
        Segment[] week7 = { S(Type.WARMUP, 300), S(Type.JOG, 1500) };
        Segment[] week8 = { S(Type.WARMUP, 300), S(Type.JOG, 1680) };
        Segment[] week9 = { S(Type.WARMUP, 300), S(Type.JOG, 1800) };
        c25kWorkoutSegmentData = new Segment[][] {
                //*/testWorkout, /*
                week1, //*/
                week1,
                week1,
                week2,
                week2,
                week2,
                week3,
                week3,
                week3,
                week4,
                week4,
                week4,
                w5d1,
                w5d2,
                w5d3,
                w6d1,
                w6d2,
                w6d3,
                week7,
                week7,
                week7,
                week8,
                week8,
                week8,
                week9,
                week9,
                week9
        };
    }
}
