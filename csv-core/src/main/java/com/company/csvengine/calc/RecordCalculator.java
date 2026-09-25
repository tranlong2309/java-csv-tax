package com.company.csvengine.calc;
import java.util.Map;
public interface RecordCalculator<T> {
    T calculate(Map<String, String> rawRow);
}
