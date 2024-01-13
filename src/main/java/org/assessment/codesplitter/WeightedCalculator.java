package org.assessment.codesplitter;

import org.assessment.codesplitter.PointCalculator;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;

public class WeightedCalculator extends PointCalculator {

    private final double ifWeight;
    private final double whileWeight;
    private final double forWeight;
    private final double statementWeight;
    private final HashMap<Integer, HashMap<Integer, Integer>> map;

    public WeightedCalculator(File refCode, double ifWeight, double whileWeight, double forWeight, double statementWeight) throws FileNotFoundException {
        super(refCode);
        this.ifWeight = ifWeight;
        this.whileWeight = whileWeight;
        this.forWeight = forWeight;
        this.statementWeight = statementWeight;
        this.map = new HashMap<>();
        for (int i = 0; i < refCodeSize(); i++) {
            map.put(i, new HashMap<>());
            for (int j = 0; j < refCodeList.get(i).size(); j++) {
                String currentLine = refCodeList.get(i).get(j);
                if (currentLine.contains("if")) {
                    map.get(i).put(0, map.get(i).getOrDefault(0, 0) + 1);
                } else if (currentLine.contains("while")) {
                    map.get(i).put(1, map.get(i).getOrDefault(1, 0) + 1);
                } else if (currentLine.contains("for")) {
                    map.get(i).put(2, map.get(i).getOrDefault(2, 0) + 1);
                }
            }
        }
    }

    @Override
    public double calculate(ArrayList<Integer> splits, ArrayList<String> file) {
        double total = 0;
        for (int i = 0; i < splits.size() - 1; i++) {
            int ifCounter = 0;
            int whileCounter = 0;
            int forCounter = 0;
            for (int j = splits.get(i); j < splits.get(i + 1); j++) {
                String currentLine = file.get(j);
                if (currentLine.contains("if")) {
                    ifCounter++;
                    if (map.get(i).getOrDefault(0, 0) >= ifCounter) {
                        total += ifWeight;
                    }
                } else if (currentLine.contains("while")) {
                    whileCounter++;
                    if (map.get(i).getOrDefault(1, 0) >= whileCounter) {
                        total += whileWeight;
                    }
                }  else if (currentLine.contains("for")) {
                    forCounter++;
                    if (map.get(i).getOrDefault(2, 0) >= forCounter) {
                        total += forWeight;
                    }
                } else {
                    total += statementWeight;
                }
            }
        }
        int ifCounter = 0;
        int whileCounter = 0;
        int forCounter = 0;
        for (int i = splits.get(splits.size() - 1); i < file.size(); i++) {
            String currentLine = file.get(i);
            if (currentLine.contains("if")) {
                ifCounter++;
                if (map.get(splits.size() - 1).getOrDefault(0, 0) >= ifCounter) {
                    total += ifWeight;
                }
            } else if (currentLine.contains("while")) {
                whileCounter++;
                if (map.get(splits.size() - 1).getOrDefault(1, 0) >= whileCounter) {
                    total += whileWeight;
                }
            }  else if (currentLine.contains("for")) {
                forCounter++;
                if (map.get(splits.size() - 1).getOrDefault(2, 0) >= forCounter) {
                    total += forWeight;
                }
            } else {
                total += statementWeight;
            }
        }
        return total;
    }
}
