package org.assessment;

import java.util.*;

public class Splitter {

    private static boolean bracesCheck(ArrayList<Pair<Command, Integer>> commands) {
        int open = 0, close = 0;
        for (Pair<Command, Integer> command : commands) {
            if (command.getKey().equals(Command.OPEN)) {
                open++;
            } else if (command.getKey().equals(Command.CLOSE)) {
                close++;
            }
        }
        return open == close;
    }

    private static ArrayList<Pair<Command, Integer>> createCommands(ArrayList<String> lines) {
        ArrayList<Pair<Command, Integer>> commands = new ArrayList<>();
        int i = 1;
        if (lines.get(i).trim().equals("{")) {
            i++;
        }
        while (i < lines.size() - 1) {
            if (lines.get(i).contains("}")) {
                commands.add(new Pair<>(Command.CLOSE, i));
            }
            if (lines.get(i).contains("{")) {
                if (lines.get(i).trim().equals("{")) {
                    commands.remove(commands.size() - 1);
                    commands.add(new Pair<>(Command.OPEN, i - 1));
                } else {
                    commands.add(new Pair<>(Command.OPEN, i));
                }
            } else {
                if (!lines.get(i).isEmpty() && !lines.get(i).contains("}")) {
                    commands.add(new Pair<>(Command.STATEMENT, i));
                }
            }
            i++;
        }
        return commands;
    }

    private static int solve(int curIndex, ArrayList<Pair<Command, Integer>> commands, ArrayList<Integer> splitLines) {
        ArrayList<Integer> tmp = new ArrayList<>();
        for (int i = curIndex + 1; i < commands.size(); i++) {
            if (commands.get(i).getKey().equals(Command.CLOSE)) {
                if (tmp.size() > 1) {
                    for (Integer integer : tmp) {
                        if (integer != -1) {
                            splitLines.add(integer);
                        }
                    }
                    splitLines.add(commands.get(curIndex).getValue());
                }
                return i;
            } else if (commands.get(i).getKey().equals(Command.OPEN)) {
                i = solve(i, commands, splitLines);
                tmp.add(-1);
            } else {
                tmp.add(commands.get(i).getValue());
            }
        }
        return -1;
    }

    public static ArrayList<Integer> split(ArrayList<String> lines) throws BracesNotMatchException {
        ArrayList<Integer> splitLines = new ArrayList<>();
        splitLines.add(0);
        ArrayList<Pair<Command, Integer>> commands = createCommands(lines);
        if (bracesCheck(commands)) {
            for (int i = 0; i < commands.size(); i++) {
                if (commands.get(i).getKey().equals(Command.OPEN)) {
                    i = solve(i, commands, splitLines);
                } else {
                    splitLines.add(commands.get(i).getValue());
                }
            }
        } else {
            throw new BracesNotMatchException(lines);
        }
        Collections.sort(splitLines);
        return splitLines;
    }
}