package org.assessment.codesplitter;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Scanner;

public class CodeSplitter {

    private static ArrayList<Integer> findCandidates(ArrayList<Integer> possibilities, ArrayList<Integer> splits, HashMap<Integer, Integer> map) {
        ArrayList<Integer> candidates = new ArrayList<>();
        int i = 0;
        if (!splits.isEmpty()) {
            i = map.get(splits.get(splits.size() - 1)) + 1;
        }
        for (; i < possibilities.size(); i++) {
            candidates.add(possibilities.get(i));
        }
        return candidates;
    }

    private static Pair<ArrayList<Integer>, Double> backtrack(ArrayList<Integer> splits, PointCalculator calculator, ArrayList<Integer> possibilities, ArrayList<String> codeLines, HashMap<Integer, Integer> map) throws CloneNotSupportedException {
        ArrayList<Integer> candidates = findCandidates(possibilities, splits, map);
        if (candidates.isEmpty() || calculator.refCodeSize() == splits.size()) {
            return new Pair<>((ArrayList<Integer>) splits.clone(), calculator.calculate(splits, codeLines));
        }
        Pair<ArrayList<Integer>, Double> pair = new Pair<>(null, (double) Integer.MIN_VALUE);
        for (Integer candidate : candidates) {
            splits.add(candidate);
            Pair<ArrayList<Integer>, Double> p = backtrack(splits, calculator, possibilities, codeLines, map);
            if (pair.getValue() < p.getValue()) {
                pair = p.clone();
            }
            splits.remove(splits.size() - 1);
        }
        return pair;
    }

    public static ArrayList<ArrayList<Integer>> calculateBestSplits(String folderName, PointCalculator calculator) throws FileNotFoundException, CloneNotSupportedException, BracesNotMatchException {
        File folder = new File(folderName);
        File[] listOfFiles = folder.listFiles();
        ArrayList<ArrayList<Integer>> splitLines = new ArrayList<>();
        for (int i = 0; i < Objects.requireNonNull(listOfFiles).length; i++) {
            if (listOfFiles[i].isFile() && !listOfFiles[i].getName().equals("RefCode.java")) {
                ArrayList<String> codeLines = new ArrayList<>();
                Scanner source = new Scanner(listOfFiles[i]);
                while (source.hasNext()) {
                    codeLines.add(source.nextLine());
                }
                source.close();
                ArrayList<Integer> possibilities = Splitter.split(codeLines);
                HashMap<Integer, Integer> map = new HashMap<>();
                for (int j = 0; j < possibilities.size(); j++) {
                    map.put(possibilities.get(j), j);
                }
                Pair<ArrayList<Integer>, Double> bestPair = backtrack(new ArrayList<>(), calculator, possibilities, codeLines, map);
                splitLines.add(bestPair.getKey());
                //System.out.println(listOfFiles[i].getName() + " is done with " + bestPair.getValue() + " accuracy.");
            }
        }
        return splitLines;
    }

    public static ArrayList<ArrayList<Integer>> calculateBestSplitsforFile(File fileName, PointCalculator calculator) throws FileNotFoundException, CloneNotSupportedException, BracesNotMatchException {
        ArrayList<String> codeLines = new ArrayList<>();
        Scanner source = new Scanner(fileName);
        ArrayList<ArrayList<Integer>> splitLines = new ArrayList<>();
        while (source.hasNext()) {
            codeLines.add(source.nextLine());
        }
        source.close();
        ArrayList<Integer> possibilities = Splitter.split(codeLines);
        HashMap<Integer, Integer> map = new HashMap<>();
        for (int j = 0; j < possibilities.size(); j++) {
            map.put(possibilities.get(j), j);
        }
        Pair<ArrayList<Integer>, Double> bestPair = backtrack(new ArrayList<>(), calculator, possibilities, codeLines, map);
        splitLines.add(bestPair.getKey());
        //System.out.println(listOfFiles[i].getName() + " is done with " + bestPair.getValue() + " accuracy.");
        return splitLines;
    }
}
