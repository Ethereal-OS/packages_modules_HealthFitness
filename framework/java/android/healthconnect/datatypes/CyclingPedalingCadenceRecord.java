/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.annotation.NonNull;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

/** Captures the user's cycling pedaling cadence. */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_CYCLING_PEDALING_CADENCE)
public final class CyclingPedalingCadenceRecord extends IntervalRecord {
    private final List<CyclingPedalingCadenceRecordSample> mCyclingPedalingCadenceRecordSamples;

    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param startTime Start time of this activity
     * @param startZoneOffset Zone offset of the user when the activity started
     * @param endTime End time of this activity
     * @param endZoneOffset Zone offset of the user when the activity finished
     * @param cyclingPedalingCadenceRecordSamples Samples of recorded CyclingPedalingCadenceRecord
     */
    private CyclingPedalingCadenceRecord(
            @NonNull Metadata metadata,
            @NonNull Instant startTime,
            @NonNull ZoneOffset startZoneOffset,
            @NonNull Instant endTime,
            @NonNull ZoneOffset endZoneOffset,
            @NonNull List<CyclingPedalingCadenceRecordSample> cyclingPedalingCadenceRecordSamples) {
        super(metadata, startTime, startZoneOffset, endTime, endZoneOffset);
        Objects.requireNonNull(metadata);
        Objects.requireNonNull(startTime);
        Objects.requireNonNull(startZoneOffset);
        Objects.requireNonNull(startTime);
        Objects.requireNonNull(endZoneOffset);
        Objects.requireNonNull(cyclingPedalingCadenceRecordSamples);
        mCyclingPedalingCadenceRecordSamples = cyclingPedalingCadenceRecordSamples;
    }

    /**
     * @return CyclingPedalingCadenceRecord samples corresponding to this record
     */
    @NonNull
    public List<CyclingPedalingCadenceRecordSample> getSamples() {
        return mCyclingPedalingCadenceRecordSamples;
    }

    /** Represents a single measurement of the cycling pedaling cadence. */
    public static final class CyclingPedalingCadenceRecordSample {
        private final double mRevolutionsPerMinute;
        private final Instant mTime;

        /**
         * CyclingPedalingCadenceRecord sample for entries of {@link CyclingPedalingCadenceRecord}
         *
         * @param revolutionsPerMinute Cycling revolutions per minute.
         * @param time The point in time when the measurement was taken.
         */
        public CyclingPedalingCadenceRecordSample(
                double revolutionsPerMinute, @NonNull Instant time) {
            Objects.requireNonNull(time);
            mTime = time;
            mRevolutionsPerMinute = revolutionsPerMinute;
        }

        /**
         * @return RevolutionsPerMinute for this sample
         */
        public double getRevolutionsPerMinute() {
            return mRevolutionsPerMinute;
        }

        /**
         * @return time at which this sample was recorded
         */
        @NonNull
        public Instant getTime() {
            return mTime;
        }

        /**
         * Indicates whether some other object is "equal to" this one.
         *
         * @param object the reference object with which to compare.
         * @return {@code true} if this object is the same as the obj
         */
        @Override
        public boolean equals(@NonNull Object object) {
            if (super.equals(object) && object instanceof CyclingPedalingCadenceRecordSample) {
                CyclingPedalingCadenceRecordSample other =
                        (CyclingPedalingCadenceRecordSample) object;
                return this.getRevolutionsPerMinute() == other.getRevolutionsPerMinute()
                        && this.getTime().equals(other.getTime());
            }
            return false;
        }

        /**
         * Returns a hash code value for the object.
         *
         * @return a hash code value for this object.
         */
        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), this.getRevolutionsPerMinute(), this.getTime());
        }
    }

    /** Builder class for {@link CyclingPedalingCadenceRecord} */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mStartTime;
        private final Instant mEndTime;
        private final List<CyclingPedalingCadenceRecordSample> mCyclingPedalingCadenceRecordSamples;
        private ZoneOffset mStartZoneOffset;
        private ZoneOffset mEndZoneOffset;

        /**
         * @param metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param startTime Start time of this activity
         * @param endTime End time of this activity
         * @param cyclingPedalingCadenceRecordSamples Samples of recorded
         *     CyclingPedalingCadenceRecord
         */
        public Builder(
                @NonNull Metadata metadata,
                @NonNull Instant startTime,
                @NonNull Instant endTime,
                @NonNull
                        List<CyclingPedalingCadenceRecordSample>
                                cyclingPedalingCadenceRecordSamples) {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(startTime);
            Objects.requireNonNull(endTime);
            Objects.requireNonNull(cyclingPedalingCadenceRecordSamples);
            mMetadata = metadata;
            mStartTime = startTime;
            mEndTime = endTime;
            mStartZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
            mEndZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
            mCyclingPedalingCadenceRecordSamples = cyclingPedalingCadenceRecordSamples;
        }

        /** Sets the zone offset of the user when the activity started */
        @NonNull
        public Builder setStartZoneOffset(@NonNull ZoneOffset startZoneOffset) {
            Objects.requireNonNull(startZoneOffset);
            mStartZoneOffset = startZoneOffset;
            return this;
        }

        /** Sets the zone offset of the user when the activity ended */
        @NonNull
        public Builder setEndZoneOffset(@NonNull ZoneOffset endZoneOffset) {
            Objects.requireNonNull(endZoneOffset);
            mEndZoneOffset = endZoneOffset;
            return this;
        }

        /**
         * @return Object of {@link CyclingPedalingCadenceRecord}
         */
        @NonNull
        public CyclingPedalingCadenceRecord build() {
            return new CyclingPedalingCadenceRecord(
                    mMetadata,
                    mStartTime,
                    mStartZoneOffset,
                    mEndTime,
                    mEndZoneOffset,
                    mCyclingPedalingCadenceRecordSamples);
        }
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param object the reference object with which to compare.
     * @return {@code true} if this object is the same as the obj
     */
    @Override
    public boolean equals(@NonNull Object object) {
        if (super.equals(object) && object instanceof CyclingPedalingCadenceRecord) {
            CyclingPedalingCadenceRecord other = (CyclingPedalingCadenceRecord) object;
            return this.getSamples().equals(other.getSamples());
        }
        return false;
    }

    /** Returns a hash code value for the object. */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), this.getSamples());
    }
}