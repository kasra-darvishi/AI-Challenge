package client;

import client.model.*;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class AI
{

    private Logger logger;
    private Random random = new Random();

    public void preProcess(World world)
    {
        logger.debug("\n\n----------------      preProcess      ----------------\n\n");
        long t1 = System.currentTimeMillis();

        PropertyConfigurator.configure("log4j.properties");
        logger = Logger.getLogger("GoodLog");

        long t2 = System.currentTimeMillis();
        logger.debug("elapsed time: " + (t2 - t1));
    }

    public void pickTurn(World world)
    {
        logger.debug("\n\n----------------      pickTurn      ----------------\n\n");
        long t1 = System.currentTimeMillis();

        world.pickHero(HeroName.values()[world.getCurrentTurn()]);

        long t2 = System.currentTimeMillis();
        logger.debug("elapsed time: " + (t2 - t1));
    }

    public void moveTurn(World world)
    {
        logger.debug("\n\n----------------      moveTurn      ----------------\n\n");
        long t1 = System.currentTimeMillis();
        Hero[] heroes = world.getMyHeroes();
        Cell[] objectiveZones = world.getMap().getObjectiveZone();
        List<Cell> blockedCells = new ArrayList<>();

        int i = 0;
        for (Hero hero : heroes)
        {
            logger.debug("hero: " + hero.toString());
            logger.debug("position: " + hero.getCurrentCell().toString());

            Direction[] dirs = world.getPathMoveDirections(hero.getCurrentCell(), objectiveZones[i], blockedCells);
//            System.out.println("size: " + dirs.length);
            if (dirs.length != 0) {
                world.moveHero(hero, dirs[0]);
                Cell c;
                int row = hero.getCurrentCell().getRow();
                int col = hero.getCurrentCell().getColumn();
                if (dirs[0] == Direction.UP)
                    c = world.getMap().getCell(row + 1, col);
                else if (dirs[0] == Direction.RIGHT)
                    c = world.getMap().getCell(row, col + 1);
                else if (dirs[0] == Direction.DOWN)
                    c = world.getMap().getCell(row - 1, col);
                else
                    c = world.getMap().getCell(row, col - 1);
                logger.debug("added to blocked list (for future move): " + c.toString());
                blockedCells.add(c);
            }else{
//                System.out.println("empty " + i);
                blockedCells.add(hero.getCurrentCell());
                logger.debug("added to blocked list (for standing): " + hero.getCurrentCell().toString());
            }

            i++;
        }

        long t2 = System.currentTimeMillis();
        logger.debug("elapsed time: " + (t2 - t1));
    }

    public void actionTurn(World world) {
        logger.debug("\n\n----------------      actionTurn      ----------------\n\n");
        long t1 = System.currentTimeMillis();

        System.out.println("action started");

        Hero[] myHeros = world.getMyHeroes();
        Hero[] enemyHeros = world.getOppHeroes();
        List<Pair<Hero, Ability>[]> allPossibleAbilities = new ArrayList<>();
        List<Pair<Hero, Pair<Ability, Cell>>> allPossibleActions = new ArrayList<>();
        int heroIndex = 0;

        // check all the possible actions
        for (Ability ability1: myHeros[heroIndex].getAbilities()){
            for (Ability ability2: myHeros[heroIndex + 1].getAbilities()){
                for (Ability ability3: myHeros[heroIndex + 2].getAbilities()){
                    for (Ability ability4: myHeros[heroIndex + 3].getAbilities()){
                        for (Pair<Hero, Ability>[] orderedAbilities: getAllPossibleOrders(ability1, ability2, ability3, ability4, myHeros)){
                            makeValid(orderedAbilities, world.getAP());
                            allPossibleAbilities.add(orderedAbilities);
                        }
                    }
                }
            }
        }

        // find best sequence of actions and their best target cells
        double mostExpectedScore = Double.MIN_VALUE;
        Pair<Hero, Pair<Ability, Cell>>[] bestAction = null;
        for (Pair<Hero, Ability>[] orderedAbilities: allPossibleAbilities) {
            Pair<Double, Pair<Hero, Pair<Ability, Cell>>[]> tmp = findBestTargetCells(orderedAbilities);
            double tempScore = tmp.getFirst();
            if (tempScore > mostExpectedScore) {
                mostExpectedScore = tempScore;
                bestAction = tmp.getSecond();
            }
        }

        // send the actions to sever
        for (Pair<Hero, Pair<Ability, Cell>> pair: bestAction)
            world.castAbility(pair.getFirst(), pair.getSecond().getFirst(), pair.getSecond().getSecond());

        long t2 = System.currentTimeMillis();
        logger.debug("elapsed time: " + (t2 - t1));
    }

    /**
     * check all possible target cells for abilities and compute the score based on expected damages
     */
    private Pair<Double, Pair<Hero, Pair<Ability, Cell>>[]> findBestTargetCells(Pair<Hero, Ability>[] orderedAbilities) {
        return null;
    }

    /**
     * validate the sequence of actions based on remaining action point
     */
    private void makeValid(Pair<Hero, Ability>[] orderedAbilities, int actionPoint) {

    }

    /**
     * sort the abilities by the order that server performs them and return all possible orders
     */
    private List<Pair<Hero, Ability>[]> getAllPossibleOrders(Ability ability1, Ability ability2, Ability ability3, Ability ability4, Hero[] heros) {
        return new ArrayList<>();
    }

    /**
     *
     */
    public double expectedDamage(World world){
        world.getOppCastAbilities()[0].getAbilityName();
        return 0.0;
    }

    /**
     *
     */
    public double expectedScore(World world){

        return 0.0;
    }

}