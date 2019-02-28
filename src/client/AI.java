package client;

import client.model.*;
import client.model.Map;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.util.*;

public class AI
{

    private Logger logger;
    private Random random = new Random();
    private int enemy_AP;
    private boolean doOnce, shouldUpdateCoolDowns = true;
    private java.util.Map<Integer, java.util.Map<AbilityName, Integer>> coolDownMap;
    private boolean debugMode = false;

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
                int minDistToEnemy = Integer.MAX_VALUE;
                for (Hero enemy: world.getOppHeroes()){
                    if (enemy.getCurrentCell().getRow() == -1 || enemy.getCurrentCell().getColumn() == -1)
                        continue;
                    for (Hero ally: world.getMyHeroes()){
                        minDistToEnemy = Integer.min(world.manhattanDistance(enemy.getCurrentCell(), ally.getCurrentCell()), minDistToEnemy);
                    }
                }

                if (minDistToEnemy > 3){
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
                }
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
        Hero[] allyHeroes = world.getMyHeroes();
        logger.debug("\nstate of our heroes:");
        for (Hero hero: allyHeroes)
            logger.debug(hero);
        Hero[] enemyHeroes = world.getOppHeroes();
        logger.debug("\nposition of allies: ");
        for (Hero hero: allyHeroes) {
            logger.debug("hero: " + hero.getName() + " row: " + hero.getCurrentCell().getRow() + " col: " + hero.getCurrentCell().getColumn());
        }
        logger.debug("\nposition of enemies: ");
        for (Hero hero: enemyHeroes) {
            logger.debug("hero: " + hero.getName() + " row: " + hero.getCurrentCell().getRow() + " col: " + hero.getCurrentCell().getColumn());
        }
        List<Pair<Hero, Ability>[]> setsOfActions = new ArrayList<>();
        List<Pair<Hero, Ability>[]> setsOfActions_Enemy = new ArrayList<>();
        Ability[] abilities = new Ability[4];

        // check all the possible set of actions
        getAllPossibleActions(setsOfActions, allyHeroes, 0, world.getAP(), false, abilities, world.getAP());
//        int i = 1;
//        for (Pair<Hero, Ability>[] pairs: setsOfActions){
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

        getAllPossibleActions(setsOfActions_Enemy, enemyHeroes, 0, enemy_AP, true, abilities, enemy_AP);
//        int i = 1;
//        for (Pair<Hero, Ability>[] pairs: setsOfActions_Enemy){
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

        logger.debug("number of action sets of ally: " + setsOfActions.size());
        logger.debug("number of action sets of enemy: " + setsOfActions_Enemy.size());

        // expected damage dealt to enemy heroes
        List<java.util.Map<Integer, Double>> ed_toEnemy= new ArrayList<>();
        totalExpectedDamage(ed_toEnemy, allyHeroes, enemyHeroes, setsOfActions);
       // expected damage dealt to our heroes
        List<java.util.Map<Integer, Double>> ed_toAlly = new ArrayList<>();
        totalExpectedDamage(ed_toAlly, enemyHeroes, allyHeroes, setsOfActions_Enemy);

        // find best sequence of actions and their best target cells
        double maxScore = -Double.MAX_VALUE;
        double minScore = Double.MAX_VALUE;
        List<Pair<Hero, Pair<Ability, Cell>>> bestAction = new ArrayList<>();
        int i = 0, j = 0;

        for (Pair<Hero, Ability>[] setOfAllyActions: setsOfActions) {
            List<Pair<Hero, Pair<Ability, Cell>>> allyAction = null;
            boolean didItOnce = false;
            for (Pair<Hero, Ability>[] setOfEnemyActions: setsOfActions_Enemy) {
                // TODO: check the correctness of indexes && if the putAll solves the problem of changing the reference
                java.util.Map<Integer, Double> estimatedDamageToEnemy = ed_toEnemy.get(i);
                java.util.Map<Integer, Double> estimatedDamageToAlly = ed_toAlly.get(j);
                if (!didItOnce) {
//                    logger.debug("\n\nchoose cell for me");
                    allyAction = getBestTargetCells(world, setOfAllyActions, estimatedDamageToAlly, allyHeroes, enemyHeroes);
//                    for (Pair<Hero, Pair<Ability, Cell>> pair: allyAction){
//                        logger.debug("\nhero: " + pair.getFirst());
//                        logger.debug("ability: " + pair.getSecond().getFirst().getName() + " row: " + pair.getSecond().getSecond().getRow() + " col: " + pair.getSecond().getSecond().getColumn());
//                    }
                    didItOnce = true;
                }
//                logger.debug("choose cell for enemy");
                List<Pair<Hero, Pair<Ability, Cell>>> enemyAction = getBestTargetCells(world, setOfEnemyActions, estimatedDamageToEnemy, enemyHeroes, allyHeroes);
                double score = getScore(world, allyAction, estimatedDamageToEnemy, enemyAction, estimatedDamageToAlly);
//                logger.debug("------ score: " + score);
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
            i++;
            j = 0;
        }

        logger.debug("\n\n\nmaxScore: " + maxScore);
        if (bestAction != null) {
            for (Pair<Hero, Pair<Ability, Cell>> p: bestAction)
                logger.debug("hero: " + p.getFirst().getName() + " ability: " + p.getSecond().getFirst().getName() + " cell: row: "
                        + p.getSecond().getSecond().getRow() + " col: " + p.getSecond().getSecond().getColumn());
        }
        // send the actions to server
        for (Pair<Hero, Pair<Ability, Cell>> pair: bestAction)
            world.castAbility(pair.getFirst(), pair.getSecond().getFirst(), pair.getSecond().getSecond());

        enemy_AP = world.getMaxAP();
        shouldUpdateCoolDowns = true;

        long t2 = System.currentTimeMillis();
        logger.debug("elapsed time: " + (t2 - t1));
    }

    private double getScore(World world, List<Pair<Hero, Pair<Ability, Cell>>> allyAction, java.util.Map<Integer, Double> estimatedDamageToEnemy, List<Pair<Hero, Pair<Ability, Cell>>> enemyAction, java.util.Map<Integer, Double> estimatedDamageToAlly) {

        java.util.Map<Integer, Double> tmp_estimatedDamageToEnemy = new HashMap<>(estimatedDamageToEnemy);
        java.util.Map<Integer, Double> tmp_estimatedDamageToAlly = new HashMap<>(estimatedDamageToAlly);
        // consider the effect of healing and fortifying
        for (Pair<Hero, Pair<Ability, Cell>> actionPair: enemyAction){
            AbilityName an = actionPair.getSecond().getFirst().getName();
            if (an == AbilityName.GUARDIAN_FORTIFY){
                Hero targetHero = world.getOppHero(actionPair.getSecond().getSecond());
                // fortified hero takes no damage
                tmp_estimatedDamageToEnemy.replace(targetHero.getId(), 0.0);
            }else if (an == AbilityName.HEALER_HEAL){
                Hero targetHero = world.getOppHero(actionPair.getSecond().getSecond());
                // reduce the damage equal to healing power
                double oldDamage = tmp_estimatedDamageToEnemy.get(targetHero.getId());
                double newDamage = oldDamage - actionPair.getSecond().getFirst().getPower();
                // heal effect can not be more than reduced HP
                if (newDamage < 0) {
                    double reducedHP = targetHero.getMaxHP() - targetHero.getCurrentHP();
                    double tmp = Double.min(reducedHP, Math.abs(newDamage));
                    newDamage = -tmp;
                }
                tmp_estimatedDamageToEnemy.replace(targetHero.getId(), newDamage);
            }else if (an == AbilityName.BLASTER_DODGE || an == AbilityName.GUARDIAN_DODGE || an == AbilityName.HEALER_DODGE || an == AbilityName.SENTRY_DODGE){
                // assume that hero could ran away
                estimatedDamageToEnemy.replace(actionPair.getFirst().getId(), 0.0);
            }
        }
        // consider the effect of healing and fortifying
        for (Pair<Hero, Pair<Ability, Cell>> actionPair: allyAction){
            AbilityName an = actionPair.getSecond().getFirst().getName();
            if (an == AbilityName.GUARDIAN_FORTIFY){
                Hero targetHero = world.getMyHero(actionPair.getSecond().getSecond());
                // fortified hero takes no damage
                tmp_estimatedDamageToAlly.replace(targetHero.getId(), 0.0);
            }else if (an == AbilityName.HEALER_HEAL){
                Hero targetHero = world.getMyHero(actionPair.getSecond().getSecond());
                // reduce the damage equal to healing power
                double oldDamage = tmp_estimatedDamageToAlly.get(targetHero.getId());
                double newDamage = oldDamage - actionPair.getSecond().getFirst().getPower();
                // heal effect can not be more than reduced HP
                if (newDamage < 0) {
                    double reducedHP = targetHero.getMaxHP() - targetHero.getCurrentHP();
                    double tmp = Double.min(reducedHP, Math.abs(newDamage));
                    newDamage = -tmp;
                }
                tmp_estimatedDamageToAlly.replace(targetHero.getId(), newDamage);
            }else if (an == AbilityName.BLASTER_DODGE || an == AbilityName.GUARDIAN_DODGE || an == AbilityName.HEALER_DODGE || an == AbilityName.SENTRY_DODGE){
                // assume that hero could ran away
                tmp_estimatedDamageToAlly.replace(actionPair.getFirst().getId(), 0.0);
            }
        }

//        double totalDamageToEnemy2 = 0.0, totalDamageToAlly2 = 0.0;
//        for (Double d: estimatedDamageToAlly.values())
//            totalDamageToAlly2 += d;
//        for (Double d: estimatedDamageToEnemy.values())
//            totalDamageToEnemy2 += d;
//        logger.debug("\nbefore heal -> to us: " + totalDamageToAlly2 + " to them: " + totalDamageToEnemy2);

        double totalDamageToEnemy = 0.0, totalDamageToAlly = 0.0;
        for (Double d: tmp_estimatedDamageToAlly.values())
            totalDamageToAlly += d;
        for (Double d: tmp_estimatedDamageToEnemy.values())
            totalDamageToEnemy += d;
//        logger.debug("after heal -> to us: " + totalDamageToAlly + " to them: " + totalDamageToEnemy);

        return totalDamageToEnemy - totalDamageToAlly;
    }

    private List<Pair<Hero, Pair<Ability, Cell>>> getBestTargetCells(World world, Pair<Hero, Ability>[] setOfActions, java.util.Map<Integer, Double> estimatedDamageToUs, Hero[] allyHeroes, Hero[] enemyHeroes) {
        List<Pair<Hero, Pair<Ability, Cell>>> actionsAndTargets = new ArrayList<>();
        List<Cell> ignoreForDodging = new ArrayList<>();
        for (Hero ally: allyHeroes)
            ignoreForDodging.add(ally.getCurrentCell());
        for (Pair<Hero, Ability> actionPair: setOfActions){
            if (actionPair.getSecond() == null)
                continue;
//            logger.debug("\nchoose a target for: " + actionPair.getFirst());
            Cell targetCell = null;
            AbilityName an = actionPair.getSecond().getName();
            if (an == AbilityName.GUARDIAN_ATTACK || an == AbilityName.HEALER_ATTACK || an == AbilityName.SENTRY_ATTACK){
                List<Hero> inRangeHeroes = getInTargetableHeroes(actionPair.getFirst(), actionPair.getSecond(), enemyHeroes);
                if (inRangeHeroes.size() != 0) {
                    targetCell = inRangeHeroes.get(0).getCurrentCell();
                    Pair<Ability, Cell> pair1 = new Pair<>(actionPair.getSecond(), targetCell);
                    Pair<Hero, Pair<Ability, Cell>> pair2 = new Pair<>(actionPair.getFirst(), pair1);
                    actionsAndTargets.add(pair2);
                }
            }else if (an == AbilityName.BLASTER_DODGE || an == AbilityName.GUARDIAN_DODGE || an == AbilityName.HEALER_DODGE || an == AbilityName.SENTRY_DODGE){
                targetCell = whereToDodge(world.getMap(), actionPair.getFirst(), ignoreForDodging, actionPair.getSecond().getRange());
                if (targetCell != null) {
                    ignoreForDodging.add(targetCell);
                    Pair<Ability, Cell> pair1 = new Pair<>(actionPair.getSecond(), targetCell);
                    Pair<Hero, Pair<Ability, Cell>> pair2 = new Pair<>(actionPair.getFirst(), pair1);
                    actionsAndTargets.add(pair2);
                }
            }else if (an == AbilityName.BLASTER_BOMB || an == AbilityName.BLASTER_ATTACK){
                targetCell = bestPlaceForBomb(world, actionPair.getFirst(), actionPair.getSecond(), enemyHeroes);
                if (targetCell != null) {
                    Pair<Ability, Cell> pair1 = new Pair<>(actionPair.getSecond(), targetCell);
                    Pair<Hero, Pair<Ability, Cell>> pair2 = new Pair<>(actionPair.getFirst(), pair1);
                    actionsAndTargets.add(pair2);
                }
            }else if (an == AbilityName.GUARDIAN_FORTIFY){
                targetCell = whoToFortify(actionPair.getFirst(), actionPair.getSecond(), allyHeroes, estimatedDamageToUs);
                if (targetCell != null) {
                    Pair<Ability, Cell> pair1 = new Pair<>(actionPair.getSecond(), targetCell);
                    Pair<Hero, Pair<Ability, Cell>> pair2 = new Pair<>(actionPair.getFirst(), pair1);
                    actionsAndTargets.add(pair2);
                }
            }else if (an == AbilityName.HEALER_HEAL){
                targetCell = whoToHeal(actionPair.getFirst(), actionPair.getSecond(), allyHeroes, estimatedDamageToUs);
                if (targetCell != null) {
                    Pair<Ability, Cell> pair1 = new Pair<>(actionPair.getSecond(), targetCell);
                    Pair<Hero, Pair<Ability, Cell>> pair2 = new Pair<>(actionPair.getFirst(), pair1);
                    actionsAndTargets.add(pair2);
                }
            }else if (an == AbilityName.SENTRY_RAY){
                List<Hero> inRangeHeroes = getInTargetableHeroes(actionPair.getFirst(), actionPair.getSecond(), enemyHeroes);
                if (inRangeHeroes.size() != 0) {
                    targetCell = inRangeHeroes.get(0).getCurrentCell();
                    Pair<Ability, Cell> pair1 = new Pair<>(actionPair.getSecond(), targetCell);
                    Pair<Hero, Pair<Ability, Cell>> pair2 = new Pair<>(actionPair.getFirst(), pair1);
                    actionsAndTargets.add(pair2);
                }
            }
//            logger.debug("use " + actionPair.getSecond().getName() + " on row: " + (targetCell != null ? targetCell.getRow() : null) + " col: " + (targetCell != null ? targetCell.getColumn() : null));
        }

        return actionsAndTargets;
    }

    private Cell whoToHeal(Hero caster, Ability ability, Hero[] allyHeroes, java.util.Map<Integer, Double> estimatedDamageToUs) {
        double maxValue = Double.MIN_VALUE;
        Cell target = null;
        for (Hero hero: allyHeroes){
            double tmpDmg = estimatedDamageToUs.get(hero.getId());
            double value = tmpDmg + (hero.getMaxHP() - hero.getCurrentHP());
            if (value > maxValue && (Math.abs(hero.getCurrentCell().getRow() - caster.getCurrentCell().getRow()) + Math.abs(hero.getCurrentCell().getColumn() - caster.getCurrentCell().getColumn()) < ability.getRange())) {
                maxValue = value;
                target = hero.getCurrentCell();
            }
        }
        return target;
    }

    private Cell bestPlaceForBomb(World world, Hero myHero, Ability ability, Hero[] enemies){
        ArrayList<Hero> inRangeEnemies = getInTargetableHeroes(myHero, ability, enemies);

        int[][] map = new int[world.getMap().getRowNum()][world.getMap().getColumnNum()];
        for (Hero enemy:inRangeEnemies) {
            Cell enemyCell = enemy.getCurrentCell();
            int enemyRow = enemyCell.getRow();
            int enemyCol = enemyCell.getColumn();
            for (int i = -ability.getAreaOfEffect(); i<= ability.getAreaOfEffect(); i++){
                for (int j = -ability.getAreaOfEffect(); j<= ability.getAreaOfEffect(); j++){
                    if ((Math.abs(i)+Math.abs(j)) <= ability.getAreaOfEffect() &&
                            (enemyRow + i >= 0) && (enemyCol + j >= 0) && (enemyRow + i < world.getMap().getRowNum()) &&
                            (enemyCol + j < world.getMap().getColumnNum())){
                        map[enemyRow + i][enemyCol + j]++;
                    }
                }
            }
        }

        int bestI = 0;
        int bestJ = 0;
        int bestValue = 0;
        for (int i = 0; i < world.getMap().getRowNum(); i++) {
            for (int j = 0; j < world.getMap().getColumnNum(); j++) {
                if (map[i][j]> bestValue){
                    bestValue = map[i][j];
                    bestI = i;
                    bestJ = j;
                }
            }
        }
        return world.getMap().getCell(bestI, bestJ);
    }

    private Cell whoToFortify(Hero caster, Ability ability, Hero[] allyHeroes, java.util.Map<Integer, Double> estimatedDamageToUs) {
        double maxDmg = Double.MIN_VALUE;
        Cell target = null;
        for (Hero hero: allyHeroes){
            double tmpDmg = estimatedDamageToUs.get(hero.getId());
            if (tmpDmg > maxDmg && (Math.abs(hero.getCurrentCell().getRow() - caster.getCurrentCell().getRow()) + Math.abs(hero.getCurrentCell().getColumn() - caster.getCurrentCell().getColumn()) < ability.getRange())) {
                maxDmg = tmpDmg;
                target = hero.getCurrentCell();
            }
        }
        return target;
    }

    private Cell whereToDodge(Map map, Hero hero, List<Cell> ignoreForDodging, int range) {
        for (Cell cell: map.getObjectiveZone()){
            boolean shouldBeIgnored = false;
            for (Cell cell2: ignoreForDodging)
                if (cell2.getRow() == cell.getRow() && cell2.getColumn() == cell.getColumn()) {
                    shouldBeIgnored = true;
                    break;
                }
            if (!shouldBeIgnored && (Math.abs(hero.getCurrentCell().getRow() - cell.getRow()) + Math.abs(hero.getCurrentCell().getColumn() - cell.getColumn()) < range))
                return cell;
        }
        return null;
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
//            logger.debug("action set #" + (numberOfActionSet+1) + " ----123");
            Double[] tmpDamage = new Double[targets.length];
            for (int i = 0; i < targets.length; i++)
                tmpDamage[i] = 0.0;
            for (Pair<Hero, Ability> actionPair: setOfActions){
                if (actionPair.getSecond() != null && actionPair.getSecond().getType() == AbilityType.OFFENSIVE){
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
//            logger.debug("total damages:");
//            for (Double d: tmpDamage)
//                logger.debug(d);
//
//            logger.debug("result: ");
//            for (int id: tmpMap.keySet())
//                logger.debug("id: " + id + " dmg: " + tmpMap.get(id));

        }


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
                if ((remCoolDown == 0 || remCoolDown == -1)&& (remainingAP - ability.getAPCost()) >= 0) {
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
        Double[] expectedDamages = new Double[targets.length];

        /** TODO: if its bomb compute the chance of getting hit then compute the expected damage
         * and probability of lower hp heroes for getting hit is more than full hp heroes
         */
        List<Hero> inRangeHeroes = getInTargetableHeroes(caster, ability, targets);
        double numberOfAffectedHeroes = inRangeHeroes.size();
        double totalDamageOfAbility = ability.getPower();
        int i = 0;
        for (Hero hero: targets){
            boolean canBeAffected = false;
            for (Hero affectedHero: inRangeHeroes){
                if (hero.getId() == affectedHero.getId()){
                    expectedDamages[i] = totalDamageOfAbility/numberOfAffectedHeroes;
                    canBeAffected = true;
                }
            }
            if (!canBeAffected)
                expectedDamages[i] = 0.0;
            i++;
        }

//        logger.debug("caster: " + caster.getId() + " ability: " + ability.getName() + " power: " + ability.getPower());
//        i = 0;
//        for (Double d: expectedDamages){
//            logger.debug("affected: " + targets[i] + " expected damage: " + expectedDamages[i]);
//            i++;
//        }

        return expectedDamages;
    }

    private ArrayList<Hero> getInTargetableHeroes(Hero myHero, Ability ability, Hero[] enemies){
        ArrayList<Hero> inRangeHeroes = new ArrayList<Hero>();
        int row0 = myHero.getCurrentCell().getRow();
        int col0 = myHero.getCurrentCell().getColumn();

        for (Hero enemy : enemies) {
            int row = enemy.getCurrentCell().getRow();
            int col = enemy.getCurrentCell().getRow();
            double distance = Math.abs(row - row0) + Math.abs(col - col0);
            if ( row>=0 && col>=0 && distance <= (ability.getRange() + ability.getAreaOfEffect()))        // the ability can hurt this enemy
                inRangeHeroes.add(enemy);
        }
        return inRangeHeroes;
    }

    /**
     *
     */
    public double expectedScore(World world){

        return 0.0;
    }

}