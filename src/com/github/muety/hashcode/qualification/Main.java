package com.github.muety.hashcode.qualification;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// TODO: consider mentoring (currently disregarded entirely)
// TODO: multi-threading

public class Main {
    private static final int INCREMENT = 50;
    private static final float SKIP_FACTOR = 0.05f;
    private static final float BREAK_FACTOR = 0.1f;

    private static final LinkedList<Contributor> contributors = new LinkedList<>();
    private static final LinkedList<Project> projects = new LinkedList<>();

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("Final score: " + evaluate());
            dumpOutput();
        }));

        // Parse input
        parseInput();

        // Stats
        printStats();

        // Vars
        final var maxTime = projects.stream().mapToInt(p -> p.duration).sum();
        final var maxScore = projects.stream().mapToInt(p -> p.score).max().getAsInt();
        final var maxRoles = projects.stream().mapToInt(p -> p.roles.size()).max().getAsInt();
        final var maxDeadline = projects.stream().mapToInt(p -> p.deadline).max().getAsInt();

        final Comparator<Project> projectScoring = Comparator.comparing(p -> {
            var score = ((float) p.score) / maxScore;
            score *= ((float) p.roles.size()) / maxRoles;
            score *= ((float) p.deadline) / maxDeadline;
            return score;
        });

        // Simulate
        final var topProjects = projects.stream()
                .sorted(projectScoring)
                .collect(Collectors.toList());
        final LinkedList<Project> pending = topProjects.stream()
                .filter(p -> !p.done())
                .collect(Collectors.toCollection(LinkedList::new));

        final AtomicInteger logCount = new AtomicInteger(0);
        final AtomicInteger noChangesCount = new AtomicInteger(0);

        for (int t = 0; t < maxTime; t += INCREMENT) {
            final int currentTime = t;
            logCount.incrementAndGet();

            int freed = updateContributors(currentTime);
            int numAvailable = (int) contributors.stream()
                    .filter(Contributor::isAvailable)
                    .count();

            // Skip if nothing has changed since previous iteration, i.e. no contributors were freed and projects remained the same
            if (t > 0 && freed == 0 && noChangesCount.get() > 0) {
                continue;
            }

            // Skip if noone's available
            if (numAvailable == 0) {
                t += SKIP_FACTOR;
                continue;
            }

            // Abort if no changes for sufficiently long
            if (t > 0 && noChangesCount.get() >= maxTime * BREAK_FACTOR) {
                break;
            }

            noChangesCount.incrementAndGet();

            pending.removeAll(pending.stream()
                    .filter(p -> p.deadline <= currentTime) // skip expired projects
                    .filter(p -> p.duration + currentTime > p.deadline + p.score) // skip projects that will result in penalty
                    .collect(Collectors.toCollection(LinkedList::new)));

            if (pending.isEmpty()) {
                break;
            }

            if (t == 0 || logCount.get() == 10) {
                System.err.printf(
                        "[t=%d] [%.1f %%] [%.1f %% done] [%.1f %% free] Score: %d%n",
                        t + 1,
                        (((float) t + 1) / ((float) maxTime)) * 100,
                        ((float) projects.size() - pending.size()) / ((float) projects.size()) * 100,
                        ((float) numAvailable) / ((float) contributors.size()) * 100,
                        evaluate()
                );
                logCount.set(0);
            }

            final Set<Project> toClear = new HashSet<>();
            pending
                    .forEach(p -> {
                        p.clear();
                        staffProject(p, currentTime);

                        if (p.done()) {
                            toClear.add(p);
                            noChangesCount.set(0);
                        }
                    });
            pending.removeAll(toClear);
        }
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
                            new ArrayList<>()
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

    private static void dumpOutput() {
        var planned = projects.stream()
                .filter(Project::done)
                .sorted(Comparator.comparing(p -> p.startedAt))
                .collect(Collectors.toList());

        System.out.println(planned.size());

        for (final var p : planned) {
            System.out.println(p.name);
            System.out.println(p.staff.stream().map(c -> c.name).collect(Collectors.joining(" ")));
        }
    }

    private static void printStats() {
        System.out.println("====");

        System.out.println("# Projects: " + projects.size());
        System.out.println("# Contributors: " + contributors.size());
        System.out.println("# Skills: " + contributors.stream().flatMap(c -> c.skills.stream()).map(s -> s.name).collect(Collectors.toSet()).size());
        System.out.println("Max Duration: " + projects.stream().mapToInt(p -> p.duration).max().getAsInt());

        System.out.println("====");
    }

    private static int evaluate() {
        return projects.stream()
                .filter(Project::done)
                .mapToInt(p -> Math.max(0, p.score - Math.max(0, (p.startedAt + p.duration) - p.deadline)))
                .sum();
    }

    // inplace!
    private static void staffProject(Project project, int currentTime) {
        final var pool = contributors.stream()
                .filter(Contributor::isAvailable)
                .collect(Collectors.toSet());

        for (int i = 0; i < project.roles.size(); i++) {
            final var role = project.roles.get(i);
            final var index = i;
            pool.stream()
                    .filter(c -> c.skills.stream().anyMatch(s -> s.name.equals(role.name) && s.level >= role.level))
                    // find someone who has preferably exactly the right skill or is a bit overskilled
                    .min(Comparator.comparing(c -> Math.abs(c.skills.stream()
                            .filter(s -> s.name.equals(role.name))
                            .findFirst()
                            .get().level - role.level)))
                    .ifPresent(c -> {
                        c.currentProject = project;
                        c.currentRole = role;
                        pool.remove(c);
                        project.staff.set(index, c);
                        project.startedAt = currentTime;
                    });
        }

        if (!project.done()) {
            // not successful -> skip project and free employees again
            project.staff.stream().filter(Objects::nonNull).forEach(Contributor::clear);
        }
    }

    private static int updateContributors(int currentTime) {
        // find all people with a project that ends at the current time step
        final var available = contributors.stream()
                .filter(Contributor::isBusy)
                .filter(c -> c.currentProject.startedAt + c.currentProject.duration <= currentTime)
                .collect(Collectors.toUnmodifiableList());
        available.forEach(c -> {
            // conditionally level up skill
            c.skills.stream()
                    .filter(s -> s.name.equals(c.currentRole.name))
                    .filter(s -> s.level <= c.currentRole.level)
                    .findFirst()
                    .ifPresent(s -> s.level++);
            c.clear(); // free 'em
        });
        return available.size();
    }

// --- //

    enum PARSE_MODE {
        DEFAULT, CONTRIBUTOR, SKILL, PROJECT, ROLE;
    }

    static class Contributor {
        int numSkills;
        String name;
        Set<Skill> skills;

        Project currentProject = null;
        Skill currentRole = null;

        public Contributor(String name, int numSkills, Set<Skill> skills) {
            this.name = name;
            this.numSkills = numSkills;
            this.skills = skills;
        }

        public void clear() {
            currentRole = null;
            currentProject = null;
        }

        public boolean isBusy() {
            assert (currentProject != null) == (currentRole != null);
            return currentProject != null;
        }

        public boolean isAvailable() {
            return !isBusy();
        }

        @Override
        public String toString() {
            return "%s [%s]".formatted(name, String.join(", ", skills.stream().map(s -> s.name).collect(Collectors.toSet())));
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
        List<Skill> roles;

        int startedAt = -1;
        List<Contributor> staff;

        public Project(int duration, int score, int deadline, int numRoles, String name, List<Skill> roles) {
            this.duration = duration;
            this.score = score;
            this.deadline = deadline;
            this.name = name;
            this.numRoles = numRoles;
            this.roles = roles;

            clear();
        }

        public void clear() {
            this.staff = IntStream.range(0, numRoles).mapToObj(i -> ((Contributor) null)).collect(Collectors.toList());
            this.startedAt = -1;
        }

        public boolean done() {
            return staff.size() == roles.size() && staff.stream().noneMatch(Objects::isNull);
        }
    }
}
