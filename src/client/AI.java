package client;

import client.model.*;
import client.model.Map;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import sun.util.locale.provider.FallbackLocaleProviderAdapter;

import java.util.*;

public class AI
{

    private Logger logger;
    private Random random = new Random();
    private int enemy_AP;
    private boolean doOnce, shouldUpdateCoolDowns = true;
    private java.util.Map<Integer, java.util.Map<AbilityName, Integer>> coolDownMap;

    public void preProcess(World world)
    {
        long t1 = System.currentTimeMillis();

        PropertyConfigurator.configure("log4j.properties");
        logger = Logger.getLogger("GoodLog");

        logger.debug("\n\n----------------      preProcess      ----------------\n\n");

        enemy_AP = world.getMaxAP();

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
        logger.debug("\n\n----------------      moveTurn: " + world.getCurrentTurn() + " phase: " + world.getMovePhaseNum() +"      ----------------\n\n");
        long t1 = System.currentTimeMillis();

        if (!doOnce) {
            coolDownMap = new HashMap<>();
            for (Hero hero: world.getOppHeroes()){
                java.util.Map<AbilityName, Integer> tmp = new HashMap<>();
                for (Ability ability: hero.getAbilities())
                    tmp.put(ability.getName(), 0);
                coolDownMap.put(hero.getId(), tmp);
            }
            doOnce = true;
        }

        if (shouldUpdateCoolDowns) {
            updateEnemyCoolDowns(world);
            shouldUpdateCoolDowns = false;
        }
        updateEnemyAP(world);

//        logger.debug("\nmy heroes:");
//        for (Hero hero: world.getMyHeroes()){
//            logger.debug(hero.toString());
//            for (Ability ability: hero.getAbilities())
//                logger.debug(ability.toString());
//            logger.debug("\n");
//        }
//        logger.debug("\n\n\ntheir heroes:");
//        for (Hero hero: world.getOppHeroes()){
//            logger.debug(hero.toString());
//            for (Ability ability: hero.getAbilities())
//                logger.debug(ability.toString());
//            logger.debug("\n");
//        }

        Hero[] heroes = world.getMyHeroes();
        Cell[] objectiveZones = world.getMap().getObjectiveZone();
        List<Cell> blockedCells = new ArrayList<>();

        int i = 0;
        for (Hero hero : heroes)
        {
//            logger.debug("hero: " + hero.toString());
//            logger.debug("position: " + hero.getCurrentCell().toString());

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
//                logger.debug("added to blocked list (for future move): " + c.toString());
                blockedCells.add(c);
            }else{
                blockedCells.add(hero.getCurrentCell());
//                logger.debug("added to blocked list (for standing): " + hero.getCurrentCell().toString());
            }

            i++;
        }

        long t2 = System.currentTimeMillis();
        logger.debug("elapsed time: " + (t2 - t1));
    }

    public void actionTurn(World world) {
        logger.debug("\n\n----------------      actionTurn: " + world.getCurrentTurn() + "      ----------------\n\n");
        long t1 = System.currentTimeMillis();

        System.out.println("action started");
        logger.debug("remaining AP: " + world.getAP());
        Hero[] myHeroes = world.getMyHeroes();
        Hero[] enemyHeroes = world.getOppHeroes();
        List<Pair<Hero, Ability>[]> allPossibleAbilityOrders = new ArrayList<>();
        List<Pair<Hero, Ability>[]> allPossibleAbilityOrders_enemy = new ArrayList<>();
        List<Pair<Hero, Pair<Ability, Cell>>> allPossibleActions = new ArrayList<>();
        Ability[] abilities = new Ability[4];

        // check all the possible set of actions
        getAllPossibleActions(allPossibleAbilityOrders, myHeroes, 0, world.getAP(), false, abilities, world.getAP());
//        int i = 1;
//        for (Pair<Hero, Ability>[] pairs: allPossibleAbilityOrders){
//            logger.debug("\nset: " + i);
//            int tmpAP = 0;
//            for (Pair<Hero, Ability> pair: pairs) {
//                logger.debug("hero: " + pair.getFirst().getId() + " ability: " + (pair.getSecond() != null ? pair.getSecond().getName() : null));
//                if (pair.getSecond() != null)
//                    tmpAP += pair.getSecond().getAPCost();
//            }
//            logger.debug("remaining ap: " + (world.getAP() - tmpAP));
//            i++;
//        }

        getAllPossibleActions(allPossibleAbilityOrders_enemy, enemyHeroes, 0, enemy_AP, true, abilities, enemy_AP);
//        int i = 1;
//        for (Pair<Hero, Ability>[] pairs: allPossibleAbilityOrders_enemy){
//            logger.debug("\nset: " + i);
//            int tmpAP = 0;
//            for (Pair<Hero, Ability> pair: pairs) {
//                logger.debug("hero: " + pair.getFirst().getId() + " ability: " + (pair.getSecond() != null ? pair.getSecond().getName() : null));
//                if (pair.getSecond() != null)
//                    tmpAP += pair.getSecond().getAPCost();
//            }
//            logger.debug("remaining ap: " + (enemy_AP - tmpAP));
//            i++;
//        }

        // expected damage dealt to enemy heroes
        List<java.util.Map<Integer, Double>> ed_toEnemy= new ArrayList<>();
        totalExpectedDamage(ed_toEnemy, myHeroes, enemyHeroes, allPossibleAbilityOrders);
        // expected damage dealt to our heroes
        List<java.util.Map<Integer, Double>> ed_toAlly = new ArrayList<>();
        totalExpectedDamage(ed_toAlly, enemyHeroes, myHeroes, allPossibleAbilityOrders_enemy);

        // find best sequence of actions and their best target cells
        double maxScore = Double.MIN_VALUE;
        double minScore = Double.MAX_VALUE;
        Pair<Hero, Pair<Ability, Cell>>[] bestAction = null;
        int i = 0, j = 0;
        boolean didItOnce = false;
        for (Pair<Hero, Ability>[] setOfAllyActions: allPossibleAbilityOrders) {
            Pair<Hero, Pair<Ability, Cell>>[] allyAction = null;
            for (Pair<Hero, Ability>[] setOfEnemyActions: allPossibleAbilityOrders_enemy) {
                // TODO: check the correctness of indexes && if the putAll solves the problem of changing the reference
                java.util.Map<Integer, Double> estimatedDamageToEnemy = new HashMap<>(ed_toEnemy.get(i));
                java.util.Map<Integer, Double> estimatedDamageToAlly = new HashMap<>(ed_toAlly.get(j));
                if (!didItOnce) {
                    allyAction = getBestTargetCells(setOfAllyActions, estimatedDamageToAlly, enemyHeroes);
                    didItOnce = true;
                }
                Pair<Hero, Pair<Ability, Cell>>[] enemyAction = getBestTargetCells(setOfEnemyActions, estimatedDamageToEnemy, myHeroes);
                double score = getScore(world, allyAction, estimatedDamageToEnemy, enemyAction, estimatedDamageToAlly);
                if (score < minScore) {
                    minScore = score;
                }
                j++;
            }

            // assign the worst probable score that can be gained by this set of actions to it self
            if (minScore > maxScore){
                bestAction = allyAction;
                maxScore = minScore;
            }
            minScore = Double.MAX_VALUE;
            didItOnce = false;
            i++;
        }

        // send the actions to server
        for (Pair<Hero, Pair<Ability, Cell>> pair: bestAction)
            world.castAbility(pair.getFirst(), pair.getSecond().getFirst(), pair.getSecond().getSecond());

        enemy_AP = world.getMaxAP();
        shouldUpdateCoolDowns = true;

        long t2 = System.currentTimeMillis();
        logger.debug("elapsed time: " + (t2 - t1));
    }

    private double getScore(World world, Pair<Hero, Pair<Ability, Cell>>[] allyAction, java.util.Map<Integer, Double> estimatedDamageToEnemy, Pair<Hero, Pair<Ability, Cell>>[] enemyAction, java.util.Map<Integer, Double> estimatedDamageToAlly) {
        return 0;
    }

    private Pair<Hero, Pair<Ability, Cell>>[] getBestTargetCells(Pair<Hero, Ability>[] setOfActions, java.util.Map<Integer, Double> estimatedDamageToUs, Hero[] targetHeroes) {
        return new Pair[0];
    }

    /**
     * find good target cells for abilities and compute the score based on expected damages
     * and number of heroes in objective zone
     */
    private Pair<Double, Pair<Hero, Pair<Ability, Cell>>[]> getBestTargetCellsAndScore(Pair<Hero, Ability>[] setOfAllyActions, java.util.Map<Integer, Double> estimatedDamageToEnemy, Pair<Hero, Ability>[] setOfEnemyActions, java.util.Map<Integer, Double> estimatedDamageToAlly, Hero[] allyHeros, Hero[] enemyHeroes) {
        return null;
    }

    private void totalExpectedDamage(List<java.util.Map<Integer,Double>> listOfDamages, Hero[] casters, Hero[] targets, List<Pair<Hero,Ability>[]> setsOfActions) {

        for (Pair<Hero, Ability>[] setOfActions: setsOfActions){
            Double[] tmpDamage = new Double[targets.length];
            for (int i = 0; i < targets.length; i++)
                tmpDamage[i] = 0.0;
            for (Pair<Hero, Ability> actionPair: setOfActions){
                if (actionPair.getSecond().getType() == AbilityType.OFFENSIVE){
                    int i = 0;
                    for (Double d: expectedDamage(actionPair.getFirst(), targets, actionPair.getSecond())){
                        tmpDamage[i] += d;
                        i++;
                    }
                }
            }
            java.util.Map<Integer, Double> tmpMap = new HashMap<>();
            int i = 0;
            for (Hero hero: targets){
                tmpMap.put(hero.getId(), tmpDamage[i]);
                i++;
            }
            listOfDamages.add(tmpMap);
        }

    }

    private Hero[] getInRangeHeroes(Hero myHero, AbilityName abilityName, Hero[] enemies){
        return null;
    }

    /**
     * find all possible set of actions based on coolDowns and remaining action point
     */
    private void getAllPossibleActions(List<Pair<Hero, Ability>[]> possibleOrders, Hero[] heroes, int heroIndex, int remainingAP, boolean isEnemyHero, Ability[] chosenAbilities, int initialAP) {

//        logger.debug("ap: " + remainingAP + " index: " + heroIndex + " size: " + possibleOrders.size());
        int i = 0;
        for (Ability ability: heroes[heroIndex].getAbilities()){
            i++;
            if (isEnemyHero) {
                // check if ability can be cast at this time
//                logger.debug(ability.getName() + "\n");
                int remCoolDown = coolDownMap.get(heroes[heroIndex].getId()).get(ability.getName());
//                logger.debug("remainingAP: " + remainingAP + " ap cost " + ability.getAPCost() + " remCoolDown: " + remCoolDown + " AP - cost: " + (remainingAP - ability.getAPCost()) + " i: " + i);
                if (remCoolDown == 0 && (remainingAP - ability.getAPCost()) >= 0) {
                    chosenAbilities[heroIndex] = ability;
                    if (heroIndex == 3) {
                        // if one more action could be taken, this set of actions is not good enough and can be ignored
                        // TODO : check if some obviouse states can be ignored
//                        if (initialAP - remainingAP >= 60) {
                        possibleOrders.addAll(getAllPossibleOrders(chosenAbilities, heroes));
                    }else {
                        getAllPossibleActions(possibleOrders, heroes, heroIndex + 1, remainingAP - ability.getAPCost(), isEnemyHero, chosenAbilities, initialAP);
                    }
                }
            }else {
                // check if ability can be cast at this time
//                logger.debug("ap: " + remainingAP + " index: " + heroIndex + " remCoolDown: " + ability.getRemCooldown() + " AP - cost: " + (remainingAP - ability.getAPCost()) + " i: " + i);
//                logger.debug(ability.getName() + "\n");
                if (ability.getRemCooldown() == 0 && (remainingAP - ability.getAPCost()) >= 0) {
                    chosenAbilities[heroIndex] = ability;
                    if (heroIndex == 3) {
                        // if one more action could be taken, this set of actions is not good enough and can be ignored
                        // TODO : check if some obviouse states can be ignored
//                        if (initialAP - remainingAP >= 60) {
                            possibleOrders.addAll(getAllPossibleOrders(chosenAbilities, heroes));
                    }else {
                        getAllPossibleActions(possibleOrders, heroes, heroIndex + 1, remainingAP - ability.getAPCost(), isEnemyHero, chosenAbilities, initialAP);
                    }
                }
            }
        }

        // if this hero takes no actions
        chosenAbilities[heroIndex] = null;
        if (heroIndex == 3) {
            // one more action could be taken so this set of actions is not good enough and can be ignored
            // TODO : check if some obviouse states can be ignored
//            if (initialAP - remainingAP >= 60 || remainingAP < 25)
                possibleOrders.addAll(getAllPossibleOrders(chosenAbilities, heroes));
        }else
            getAllPossibleActions(possibleOrders, heroes, heroIndex + 1, remainingAP, isEnemyHero, chosenAbilities, initialAP);

    }

    /**
     * validate the sequence of actions based on remaining action point
     */
    private void makeValid(Pair<Hero, Ability>[] orderedAbilities, int actionPoint) {

    }

    /**
     * sort the abilities by the order that server performs them and return all possible orders
     */
    private List<Pair<Hero, Ability>[]> getAllPossibleOrders(Ability[] abilities, Hero[] heroes) {
        // TODO: check how the order of sending actions to server can affect the final result
        List<Pair<Hero, Ability>[]> list = new ArrayList<>();

        Pair<Hero, Ability>[] pairs = new Pair[4];
        for (int i = 0; i < 4; i++){
            pairs[i] = new Pair<>(heroes[i], abilities[i]);
        }
        list.add(pairs);

        return list;
    }

    /**
     * compute the consumed action point by tracking the enemy heroes movement
     */
    private void updateEnemyAP(World world) {



    }

    private void updateEnemyCoolDowns(World world) {

        for (java.util.Map<AbilityName, Integer> map: coolDownMap.values()){
            for (AbilityName abilityName: map.keySet()){
                int currentCoolDown = map.get(abilityName);
                if (currentCoolDown > 0)
                    map.replace(abilityName, currentCoolDown - 1);
            }
        }

        for (CastAbility castAbility: world.getOppCastAbilities()){
//            logger.debug("AbilityName: " + castAbility.getAbilityName() + " caster id: " + castAbility.getCasterId());
            int abilityCooldDown = 0;
            for (Hero hero: world.getOppHeroes())
                if (hero.getId() == castAbility.getCasterId()) {
                    abilityCooldDown = hero.getAbility(castAbility.getAbilityName()).getCooldown();
//                    logger.debug("new coolDown: " + (abilityCooldDown - 1));
                }
            if (coolDownMap.containsKey(castAbility.getCasterId()))
                coolDownMap.get(castAbility.getCasterId()).replace(castAbility.getAbilityName(), abilityCooldDown - 1);
        }

//        for (Integer heroID: coolDownMap.keySet()){
//            logger.debug("hero: " + heroID);
//            for (AbilityName abilityName: coolDownMap.get(heroID).keySet()){
//                int currentCoolDown = coolDownMap.get(heroID).get(abilityName);
//                logger.debug("hero: " + heroID + " ability: " + abilityName + " coolDown: " + currentCoolDown);
//            }
//        }

    }

    /**
     *
     */
    public Double[] expectedDamage(Hero caster, Hero[] targets, Ability ability){
        return new Double[targets.length];
    }

    /**
     *
     */
    public double expectedScore(World world){

        return 0.0;
    }

}