package org.algorythm;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Vm;
import java.util.*;

public class Flower {
    private final List<ArrayList<ArrayList<Integer>>> population = new ArrayList<>();
    private final ArrayList<ArrayList<Integer>> bestFlower = new ArrayList<>();
    private final double[][] fitness;
    private ArrayList<ArrayList<Integer>> newFLower = new ArrayList<>();
    private double[][] TimesOfVm;
    private final int average;
    private final int numberOfSolutions;
    private static final double[] ws = new double[]{ 0.5, 0.2, 0.3 };

    public List<? extends Cloudlet> cloudletList;
    public List<? extends Vm> vmsCreatedList;

    public Flower(List<? extends Cloudlet> cloudletList, List<? extends Vm> vmsCreatedList) {
        this.cloudletList = cloudletList;
        this.vmsCreatedList = vmsCreatedList;
        this.numberOfSolutions = cloudletList.size();
        fitness = new double[numberOfSolutions][2];
        this.average = (cloudletList.size() / vmsCreatedList.size()) + 1;
    }

    protected void createPopulation() {
        for (int i = 0; i < numberOfSolutions; i++) {
            population.add(new ArrayList<>());
            for (int j = 0; j < vmsCreatedList.size(); j++) {
                population.get(i).add(new ArrayList<>());
            }
        }
        for (int i = 0; i < cloudletList.size(); i++) {
            int j = (int) (Math.random() * (vmsCreatedList.size()));
            population.get(i).get(j).add(cloudletList.get(i).getCloudletId());
        }

        for (int i = 0; i < numberOfSolutions; i++) {
            int otherSol = new Random().nextInt(numberOfSolutions);
            if (otherSol != i) newFLower = pollination(population.get(i), population.get(otherSol));
            else {
                i--;
                continue;
            }
            checkNewFlower();
            population.get(i).clear();
            population.get(i).addAll(newFLower);
        }
    }

    public List<ArrayList<Integer>> start() {
        int generationNumber = 100;
        double fit;
        double newFlowerFitness;

        List<ArrayList<Integer>> answer = new ArrayList<>();
        createPopulation();

        for (int i = 0; i < numberOfSolutions; i++) {
            fitness[i][0] = i;
            fitness[i][1] = getFitness(population.get(i));
        }

        int indexOfBestFlower = getIndexOfBestSolution(fitness);
        bestFlower.addAll(population.get(indexOfBestFlower));

        for (int i = 0; i < generationNumber; i++) {
            for (int j = 0; j < numberOfSolutions; j++) {
                double probability = 0.8;
                if (Math.random() <= probability) {
                    // Local pollination
                    int other = j + 1;
                    if (other == numberOfSolutions) other = 0;
                    newFLower = pollination(population.get(j), population.get(other));
                } else {
                    // Global pollination
                    newFLower = pollination(population.get(j), bestFlower);
                }
                checkNewFlower();
                newFlowerFitness = getFitness(newFLower);

                if (newFlowerFitness < fitness[j][1]) {
                    population.get(j).clear();
                    population.get(j).addAll(newFLower);
                    fitness[j][1] = newFlowerFitness;
                }
                fit = getFitness(bestFlower);
                if (newFlowerFitness < fit) {
                    bestFlower.clear();
                    bestFlower.addAll(newFLower);
                }
            }
        }

        for (int i = 0; i < cloudletList.size(); i++) answer.add(new ArrayList<>());

        for (int i = 0; i < vmsCreatedList.size(); i++) {
            for (int j = 0; j < bestFlower.get(i).size(); j++) {
                answer.get(bestFlower.get(i).get(j)).add(i);
            }
        }
        System.out.println("\nFINAL ALLOCATIONS-");
        System.out.println(answer);
        return answer;
    }

    protected double getFitness(ArrayList<ArrayList<Integer>> flower) {
        double calculationCostTotal = 0.0;
        double calculateReliabilityTotal = 0.0;
        for (int i = 0; i < vmsCreatedList.size(); i++) {
            ArrayList<Integer> cloudletIds = flower.get(i);
            calculationCostTotal += calculateExecutionCost(cloudletIds, vmsCreatedList.get(i));
            calculateReliabilityTotal += calculateReliability(cloudletIds, vmsCreatedList.get(i));
        }
        return ws[0] * calculateMakespan(flower) + ws[1] * calculationCostTotal + ws[2] * calculateReliabilityTotal;
    }
    protected double getMaxTimeOfExecution(double[] arr) {
        double max = 0;
        for (double v : arr) if (v > max) max = v;
        return max;
    }
    public double calculateMakespan(ArrayList<ArrayList<Integer>> arr) {
        double [] vmexectime = new double[vmsCreatedList.size()];
        for (int i = 0; i < vmsCreatedList.size(); i++) {
            Vm vm = vmsCreatedList.get(i);
            double vmmips = vm.getMips();
            long totalcloudletlength = 0;
            for (int j = 0; j < arr.get(i).size(); j++) totalcloudletlength += cloudletList.get(arr.get(i).get(j)).getCloudletLength();
            double duration = (double) totalcloudletlength / vmmips;
            vmexectime[i] = duration;
        }
        return getMaxTimeOfExecution(vmexectime);
    }

    public double calculateExecutionCost(ArrayList<Integer> cloudletIds, Vm vm) {
        double totalCost = 0.0;
        for (int i : cloudletIds) {
            Cloudlet cloudlet = cloudletList.get(i);
            if (vm != null) {
                double executionDuration = cloudlet.calculateExecutionTime(vm);
                double cloudletCost = executionDuration * vm.getCostPerMips();
                totalCost += cloudletCost;
            }
        }
        return totalCost;
    }
    public double calculateReliability(ArrayList<Integer> cloudletIds, Vm vm) {
        double sum = 0.0;
        for (int i : cloudletIds) {
            Cloudlet cloudlet = cloudletList.get(i);
            sum += (cloudlet.getCloudletTotalLength() / vm.getMips()) * calculateFailureRate(cloudletIds, vm);
        }
        return Math.exp(sum * (-1));
    }
    public double calculateFailureRate(ArrayList<Integer> cloudletIds, Vm vm) {
        return 1.0;
    }
    protected int getIndexOfBestSolution(double[][] arr) {
        int index = -1;
        double bestFitness = Integer.MAX_VALUE;
        for (int i = 0; i < numberOfSolutions; i++) {
            if (arr[i][1] < bestFitness) {
                bestFitness = arr[i][1];
                index = i;
            }
        }
        return index;
    }

    protected ArrayList<ArrayList<Integer>> pollination(ArrayList<ArrayList<Integer>> flower1, ArrayList<ArrayList<Integer>> flower2) {
        ArrayList<ArrayList<Integer>> newFlower = createList(vmsCreatedList.size());
        int random = new Random().nextInt(vmsCreatedList.size());
        for (int i = 0; i < vmsCreatedList.size(); i++) {
            if (i <= random) newFlower.get(i).addAll(flower1.get(i));
            else newFlower.get(i).addAll(flower2.get(i));
        }
        return newFlower;
    }

    protected void checkNewFlower() {
        this.TimesOfVm = new double[vmsCreatedList.size()][2];
        List<ArrayList<Integer>> answer = createList(cloudletList.size());

        for (int i = 0; i < vmsCreatedList.size(); i++) {
            for (int j = 0; j < newFLower.get(i).size(); j++) {
                answer.get(newFLower.get(i).get(j)).add(i);
            }
        }
        for (int i = 0; i < cloudletList.size(); i++) {
            if (answer.get(i).size() > 1) {
                deleteMultipleAssignedCloudlet(i, answer.get(i));
            }
        }
        for (int i = 0; i < cloudletList.size(); i++) {
            if (answer.get(i).size() == 0) {
                assignUnassignedCloudlet(i);
            }
        }
    }

    protected void assignUnassignedCloudlet(int index) {
        getExecutionTimesOfVm();
        vmSortByTime(TimesOfVm);
        for (int i = 0; i < vmsCreatedList.size(); i++) {
            if (newFLower.get((int) TimesOfVm[i][0]).size() < average) {
                newFLower.get((int) TimesOfVm[i][0]).add(index);
                break;
            }
        }
    }

    protected void deleteMultipleAssignedCloudlet(int index, List<Integer> list) {
        getExecutionTimesOfVm();
        if ((TimesOfVm[list.get(0)][1] >= TimesOfVm[list.get(1)][1])) {
            for (int j = 0; j < newFLower.get(list.get(0)).size(); j++) if (newFLower.get(list.get(0)).get(j) == index) newFLower.get(list.get(0)).remove(j);
        } else if ((TimesOfVm[list.get(0)][1] <= TimesOfVm[list.get(1)][1])) {
            for (int j = 0; j < newFLower.get(list.get(1)).size(); j++) if (newFLower.get(list.get(1)).get(j) == index) newFLower.get(list.get(1)).remove(j);
        }
    }

    protected void getExecutionTimesOfVm() {
        long totalLengthOfCloudlets;
        for (int i = 0; i < vmsCreatedList.size(); i++) {
            Vm vm = vmsCreatedList.get(i);
            double vmMips = vm.getMips();
            totalLengthOfCloudlets = 0;
            for (int j = 0; j < newFLower.get(i).size(); j++) {
                totalLengthOfCloudlets += cloudletList.get(newFLower.get(i).get(j)).getCloudletLength();
            }
            double duration = (double) totalLengthOfCloudlets / vmMips;
            TimesOfVm[i][0] = i;
            TimesOfVm[i][1] = duration;
        }
    }

    protected void vmSortByTime(double[][] arr) {
        for (int i = 0; i < arr.length - 1; i++) {
            for (int j = 0; j < arr.length - i - 1; j++) {
                if (arr[j][1] > arr[j + 1][1]) {
                    double temp = arr[j][1];
                    arr[j][1] = arr[j + 1][1];
                    arr[j + 1][1] = temp;

                    double temp1 = arr[j][0];
                    arr[j][0] = arr[j + 1][0];
                    arr[j + 1][0] = temp1;
                }
            }
        }
    }
    public ArrayList<ArrayList<Integer>> createList(int size) {
        ArrayList<ArrayList<Integer>> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            list.add(new ArrayList<>());
        }
        return list;
    }
}
