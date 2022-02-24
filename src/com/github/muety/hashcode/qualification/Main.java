package com.github.muety.hashcode.qualification;

import java.util.*;

public class Main {
    private static LinkedList<Contributor> contributors = new LinkedList<>();
    private static LinkedList<Project> projects = new LinkedList<>();

    public static void main(String[] args) {
        parseInput();
        System.out.println(1);
    }

    private static void parseInput() {
        final var scanner = new Scanner(System.in);

        var mode = PARSE_MODE.DEFAULT;
        var n = 0;
        var m = 0;

        while (scanner.hasNext()) {
            final var parts = scanner.nextLine().split(" ");

            switch (mode) {
                case DEFAULT:
                    n = Integer.parseInt(parts[0]);
                    m = Integer.parseInt(parts[1]);
                    mode = PARSE_MODE.CONTRIBUTOR;
                    break;
                case CONTRIBUTOR:
                    contributors.add(new Contributor(parts[0], Integer.parseInt(parts[1]), new HashSet<>()));
                    mode = PARSE_MODE.SKILL;
                    break;
                case PROJECT:
                    projects.add(new Project(
                            Integer.parseInt(parts[1]),
                            Integer.parseInt(parts[2]),
                            Integer.parseInt(parts[3]),
                            Integer.parseInt(parts[4]),
                            parts[0],
                            new HashSet<>()
                    ));
                    mode = PARSE_MODE.ROLE;
                    break;
                case SKILL:
                    final var skill = new Skill(parts[0], Integer.parseInt(parts[1]));
                    final var contributor = contributors.getLast();
                    contributor.skills.add(skill);
                    if (contributor.skills.size() == contributor.numSkills) {
                        mode = contributors.size() == n ? PARSE_MODE.PROJECT : PARSE_MODE.CONTRIBUTOR;
                    }
                    break;
                case ROLE:
                    final var requirement = new Skill(parts[0], Integer.parseInt(parts[1]));
                    final var project = projects.getLast();
                    project.roles.add(requirement);
                    if (project.roles.size() == project.numRoles) {
                        mode = PARSE_MODE.PROJECT;
                    }
                    break;
            }
        }

    }

    enum PARSE_MODE {
        DEFAULT, CONTRIBUTOR, SKILL, PROJECT, ROLE;
    }

    static class Contributor {
        int numSkills;
        String name;
        Set<Skill> skills;

        public Contributor(String name, int numSkills, Set<Skill> skills) {
            this.name = name;
            this.numSkills = numSkills;
            this.skills = skills;
        }
    }

    static class Skill {
        String name;
        int level;

        public Skill(String name, int level) {
            this.name = name;
            this.level = level;
        }
    }

    static class Project {
        int duration;
        int score;
        int deadline;
        int numRoles;
        String name;
        Set<Skill> roles;

        public Project(int duration, int score, int deadline, int numRoles, String name, Set<Skill> roles) {
            this.duration = duration;
            this.score = score;
            this.deadline = deadline;
            this.name = name;
            this.numRoles = numRoles;
            this.roles = roles;
        }
    }
}
