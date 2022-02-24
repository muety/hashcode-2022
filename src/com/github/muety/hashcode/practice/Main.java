package com.github.muety.hashcode.practice;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// TODO: use bitsets

public class Main {
    static class Customer {
        Set<String> likedIngredients;
        Set<String> dislikedIngredients;

        public Customer() {
            this.likedIngredients = new HashSet<>();
            this.dislikedIngredients = new HashSet<>();
        }

        public boolean wouldGo(Set<String> pizzaIngredients) {
            return pizzaIngredients.containsAll(likedIngredients) && pizzaIngredients.stream().noneMatch(i -> dislikedIngredients.contains(i));
        }
    }

    static class Solution {
        Set<String> ingredients;
        long score;

        public Solution(Set<String> ingredients, long score) {
            this.ingredients = ingredients;
            this.score = score;
        }

        public long getScore() {
            return score;
        }
    }

    private static final LinkedList<Customer> customers = new LinkedList<>();
    private static final Map<String, Integer> ingredientsCount = new HashMap<>();

    public static void main(String[] args) {
        parseInput();
        //final var solution = solve();
        final var solution = solveRandom();
        System.err.printf("Found solution with %d ingredients (score: %s).%n", solution.ingredients.size(), solution.score);
        printOutput(solution);
    }

    private static void parseInput() {
        final Scanner scanner = new Scanner(System.in);

        final int numCustomers = scanner.nextInt();
        scanner.nextLine();

        IntStream.range(0, 2 * numCustomers)
                .forEach(i -> {
                    if (i % 2 == 0) {
                        customers.add(new Customer());
                    }

                    final Set<String> ingredients = Arrays.stream(scanner.nextLine().split(" ")).skip(1).collect(Collectors.toSet());
                    final Customer customer = customers.getLast();
                    final Set<String> targetSet = i % 2 == 0 ? customer.likedIngredients : customer.dislikedIngredients;
                    targetSet.addAll(ingredients);

                    ingredients
                            .forEach(ingredient -> ingredientsCount.put(ingredient, ingredientsCount.getOrDefault(ingredient, 0) + (i % 2 == 0 ? 1 : -1)));
                });
    }

    private static Solution solve() {
        final List<String> topList = ingredientsCount.entrySet().stream()
                .sorted((o1, o2) -> o2.getValue().compareTo(o1.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableList());

        return IntStream.range(1, topList.size())
                .parallel()
                .mapToObj(i -> {
                    final Set<String> ingredients = topList.stream().limit(i).collect(Collectors.toUnmodifiableSet());
                    return new Solution(ingredients, evaluate(ingredients));
                })
                .max(Comparator.comparing(Solution::getScore))
                .orElse(new Solution(Set.of(), 0));
    }

    private static Solution solveRandom() {
        final Random rand = new Random();

        AtomicReference<Solution> best = new AtomicReference<>(new Solution(Set.of(), 0));

        IntStream.iterate(0, i -> i + 1)
                .parallel()
                .forEach(i -> {
                    int choose = rand.nextInt(ingredientsCount.size());
                    List<String> allIngredients = new ArrayList<>(ingredientsCount.keySet());
                    Collections.shuffle(allIngredients, rand);
                    Set<String> ingredients = Set.copyOf(allIngredients.subList(0, choose));
                    Solution solution = new Solution(ingredients, evaluate(ingredients));
                    if (solution.score > best.get().score) {
                        best.set(solution);
                        System.err.printf("Found solution with %d ingredients (score: %s).%n", solution.ingredients.size(), solution.score);
                        //printOutput(solution);
                    }
                });

        return best.get();
    }

    private static long evaluate(Set<String> solution) {
        return customers.stream().filter(c -> c.wouldGo(solution)).count();
    }

    private static void printOutput(Solution solution) {
        System.out.printf("%d %s", solution.ingredients.size(), String.join(" ", solution.ingredients));
    }

}
