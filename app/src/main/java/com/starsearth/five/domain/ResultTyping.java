package com.starsearth.five.domain;

import android.content.Context;
import android.os.Parcel;

import com.google.firebase.database.Exclude;
import com.starsearth.five.R;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by faimac on 3/8/18.
 */

public class ResultTyping extends Result {

    public int characters_correct;
    public int characters_total_attempted;
    public int words_correct;
    public int words_total_finished;

    public int getCharacters_correct() {
        return characters_correct;
    }

    public int getWords_correct() {
        return words_correct;
    }

    public void setCharacters_correct(int characters_correct) {
        this.characters_correct = characters_correct;
    }

    public void setCharacters_total_attempted(int characters_total_attempted) {
        this.characters_total_attempted = characters_total_attempted;
    }

    public void setWords_correct(int words_correct) {
        this.words_correct = words_correct;
    }

    public void setWords_total_finished(int words_total_finished) {
        this.words_total_finished = words_total_finished;
    }

    public ResultTyping() {
        super();
    }

    public ResultTyping(Map<String, Object> map) {
        super(map);
        this.characters_correct =  ((Long) map.get("characters_correct")).intValue(); //(Integer) map.get("characters_correct");
        this.characters_total_attempted = ((Long) map.get("characters_total_attempted")).intValue();
        this.words_correct = ((Long) map.get("words_correct")).intValue();
        this.words_total_finished = ((Long) map.get("words_total_finished")).intValue();
    }

    private int getTimeMins() {
        int timeMins = (int) timeTakenMillis/60000;
        if (timeMins < 1) {
            //If time taken was less than 1 min
            timeMins = 1;
        }

        return timeMins;
    }

    public int getSpeedWPM() {
        int x = words_correct;
        int y = getTimeMins();
        return (x*y > 0) ? (words_correct/getTimeMins()) : 0;
    }

    public int getAccuracy() {
        double accuracy = (double) words_correct/words_total_finished;
        double accuracyPercentage = Math.ceil(accuracy*100);
        int accuracyPercentageInt = (int) accuracyPercentage;
        return accuracyPercentageInt;
    }

    public String getScoreSummary(Context context, boolean isPassFail, int passPercentage) {
        StringBuffer result = new StringBuffer();
        if (!isPassFail) {
            result.append(Integer.valueOf(items_correct));
        }
        else {
            if (isPassed(passPercentage)) result.append(context.getString(R.string.passed));
            else result.append(context.getString(R.string.failed));
        }
        return result.toString();
    }

    public boolean isPassed(int passPercentage) {
        boolean result = false;
        int accuracy = getAccuracy();
        if (accuracy >= passPercentage) result = true;
        return result;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = (HashMap<String, Object>) super.toMap();
        result.put("characters_correct", characters_correct);
        result.put("characters_total_attempted", characters_total_attempted);
        result.put("words_correct", words_correct);
        result.put("words_total_finished", words_total_finished);

        return result;
    }

    protected ResultTyping(Parcel in) {
        super(in);
        characters_correct = in.readInt();
        characters_total_attempted = in.readInt();
        words_correct = in.readInt();
        words_total_finished = in.readInt();
    }

    public static final Creator<ResultTyping> CREATOR = new Creator<ResultTyping>() {
        @Override
        public ResultTyping createFromParcel(Parcel in) {
            return new ResultTyping(in);
        }

        @Override
        public ResultTyping[] newArray(int size) {
            return new ResultTyping[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(characters_correct);
        dest.writeInt(characters_total_attempted);
        dest.writeInt(words_correct);
        dest.writeInt(words_total_finished);
    }
}
