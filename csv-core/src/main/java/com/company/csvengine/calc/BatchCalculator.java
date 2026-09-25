package com.company.csvengine.calc;
import java.util.List;
import java.util.Map;
public interface BatchCalculator<T> {
    List<T> calculate(List<Map<String, String>> rawRows);
}
