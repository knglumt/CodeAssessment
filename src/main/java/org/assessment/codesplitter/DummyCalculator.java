package org.assessment.codesplitter;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;

public class DummyCalculator extends PointCalculator {

    public DummyCalculator(File refCode) throws FileNotFoundException {
        super(refCode);
    }

    @Override
    public double calculate(ArrayList<Integer> splits, ArrayList<String> file) {
        return Math.random();
    }
}
