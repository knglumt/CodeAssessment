package org.assessment;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;

public class LineCalculator extends PointCalculator {

    public LineCalculator(File refCode) throws FileNotFoundException {
        super(refCode);
    }

    @Override
    public double calculate(ArrayList<Integer> splits, ArrayList<String> file) {
        double total = 0;
        for (int i = 0; i < splits.size() - 1; i++) {
            total -= Math.abs((splits.get(i + 1) - splits.get(i)) - refCodeList.get(i).size());
        }
        total -= Math.abs((file.size() - splits.get(splits.size() - 1)) - refCodeList.get(splits.size() - 1).size());
        for (int i = splits.size(); i < refCodeList.size(); i++) {
            total -= refCodeList.get(i).size();
        }
        return total;
    }
}
