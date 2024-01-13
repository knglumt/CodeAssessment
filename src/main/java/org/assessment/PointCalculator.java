package org.assessment;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Scanner;

public abstract class PointCalculator {

    protected static LinkedList<ArrayList<String>> refCodeList;

    public PointCalculator(File refCode) throws FileNotFoundException {
        Scanner source = new Scanner(refCode);
        refCodeList = new LinkedList<>();
        source.nextLine();
        while (source.hasNext()) {
            String line = source.nextLine();
            if (line.startsWith("/** ASSESSMENT")) {
                refCodeList.add(new ArrayList<>());
                while (!line.startsWith(" */")) {
                    line = source.nextLine();
                }
                line = source.nextLine();
            }
            refCodeList.getLast().add(line);
        }
        source.close();
    }

    public int refCodeSize() {
        return refCodeList.size();
    }

    public abstract double calculate(ArrayList<Integer> splits, ArrayList<String> file);
}
