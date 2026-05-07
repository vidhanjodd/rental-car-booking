package com.rentalcar.enums;

import java.util.EnumSet;
import java.util.Set;


public enum BookingStatus {

    PENDING {
        @Override
        public Set<BookingStatus> allowedTransitions() {
            return EnumSet.of(CONFIRMED, CANCELLED);
        }
    },
    CONFIRMED {
        @Override
        public Set<BookingStatus> allowedTransitions() {
            return EnumSet.of(COMPLETED, CANCELLED);
        }
    },
    CANCELLED {
        @Override
        public Set<BookingStatus> allowedTransitions() {
            return EnumSet.noneOf(BookingStatus.class);   // terminal
        }
    },
    COMPLETED {
        @Override
        public Set<BookingStatus> allowedTransitions() {
            return EnumSet.noneOf(BookingStatus.class);   // terminal
        }
    };

    public abstract Set<BookingStatus> allowedTransitions();

    public boolean canTransitionTo(BookingStatus target) {
        return allowedTransitions().contains(target);
    }
}
