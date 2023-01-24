/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.healthconnect.datatypes;

import android.annotation.IntRange;
import android.annotation.NonNull;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents particular exercise within exercise session (see {@link ExerciseSessionRecord}).
 *
 * <p>Each record contains start and end time of the exercise, exercise type and optional number of
 * repetitions.
 */
public final class ExerciseSegment {
    private final TimeInterval mInterval;

    @ExerciseSegmentType.ExerciseSegmentTypes private final int mSegmentType;

    private final int mRepetitionsCount;

    private ExerciseSegment(
            @NonNull TimeInterval interval,
            @ExerciseSegmentType.ExerciseSegmentTypes int segmentType,
            @IntRange(from = 0) int repetitionsCount) {
        Objects.requireNonNull(interval);
        mInterval = interval;

        mSegmentType = segmentType;

        if (repetitionsCount < 0) {
            throw new IllegalArgumentException("Repetitions count must be non-negative.");
        }
        mRepetitionsCount = repetitionsCount;
    }

    /*
     * Returns type of the segment, one of {@link @ExerciseSegmentType.ExerciseSegmentTypes}.
     */
    @ExerciseSegmentType.ExerciseSegmentTypes
    public int getSegmentType() {
        return mSegmentType;
    }

    /*
     * Returns number of repetitions in the current segment. Positive value.
     */
    @IntRange(from = 0)
    public int getRepetitionsCount() {
        return mRepetitionsCount;
    }

    /*
     * Returns start time of the segment.
     */
    @NonNull
    public Instant getStartTime() {
        return mInterval.getStartTime();
    }

    /*
     * Returns end time of the segment.
     */
    @NonNull
    public Instant getEndTime() {
        return mInterval.getEndTime();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExerciseSegment)) return false;
        ExerciseSegment that = (ExerciseSegment) o;
        return mSegmentType == that.mSegmentType && mRepetitionsCount == that.mRepetitionsCount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mSegmentType, mRepetitionsCount);
    }

    /** Builder class for {@link ExerciseSegment} */
    public static final class Builder {
        private final TimeInterval mInterval;

        @ExerciseSegmentType.ExerciseSegmentTypes private final int mSegmentType;

        private int mRepetitionsCount = 0;

        public Builder(
                @NonNull Instant startTime,
                @NonNull Instant endTime,
                @ExerciseSegmentType.ExerciseSegmentTypes int segmentType) {
            Objects.requireNonNull(startTime);
            Objects.requireNonNull(endTime);
            mInterval = new TimeInterval(startTime, endTime);
            mSegmentType = segmentType;
        }

        /**
         * Sets the number of repetitions to the current segment. Returns builder instance with
         * repetitions count set.
         */
        @NonNull
        public Builder setRepetitionsCount(@IntRange(from = 0) int repetitionsCount) {
            if (repetitionsCount < 0) {
                throw new IllegalArgumentException("Number of repetitions must be non negative.");
            }
            mRepetitionsCount = repetitionsCount;
            return this;
        }

        /**
         * Sets the number repetitions to the current segment. Returns {@link ExerciseSegment}
         * instance.
         */
        @NonNull
        public ExerciseSegment build() {
            return new ExerciseSegment(mInterval, mSegmentType, mRepetitionsCount);
        }
    }
}