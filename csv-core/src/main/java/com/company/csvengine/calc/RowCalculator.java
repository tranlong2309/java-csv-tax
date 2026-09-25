package com.company.csvengine.calc;
import java.util.Map;
public interface RowCalculator<T> {
    T calculate(Map<String, String> rawRow);
}
