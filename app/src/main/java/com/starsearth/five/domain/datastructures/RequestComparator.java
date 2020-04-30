package com.starsearth.five.domain.datastructures;

import com.starsearth.five.domain.HelpRequest;
import com.starsearth.five.domain.Result;

import java.util.Comparator;

/*
    This comparator is used to sort record items in reverse chronological order. ie: latest results come first
 */
public class RequestComparator implements Comparator<HelpRequest> {
    @Override
    public int compare(HelpRequest request1, HelpRequest request2) {
        Long timestamp1 = request1.timestampCompletion;
        Long timestamp2 = request2.timestampCompletion;
        return Integer.compare(timestamp2.intValue(), timestamp1.intValue());
    }
}
