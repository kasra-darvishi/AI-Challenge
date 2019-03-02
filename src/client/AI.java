package client;

import client.model.*;
import client.model.Map;

import java.util.*;

public class AI
{

    private Random random = new Random();
    private int enemyActionPoint;
    private Cell[] lastLocationOfEnemies;
    private boolean doOnce, shouldUpdateCoolDowns = true;
    private java.util.Map<Integer, java.util.Map<AbilityName, Integer>> coolDownMap;
    private boolean debugMode = false;
    private Hero targetHero;
    private int lastPhaseChoosedATarget = -1;
    private Cell chosenObjectiveCloseToUs, chosenObjectiveCloseToThem;

    public void preProcess(World world)
    {
        long t1 = System.currentTimeMillis();


        //logger.debug("\n\n----------------      preProcess      ----------------\n\n");

        double minDist = Double.MAX_VALUE;
        for (Cell cell: world.getMap().getObjectiveZone()){
            double tmp = 0.0;
            for (Cell cell1: world.getMap().getMyRespawnZone())
                tmp += world.manhattanDistance(cell, cell1);
            if (tmp/4 < minDist){
                chosenObjectiveCloseToUs = cell;
                minDist = tmp/4;
            }
        }
        minDist = Double.MAX_VALUE;
        for (Cell cell: world.getMap().getObjectiveZone()){
            double tmp = 0.0;
            for (Cell cell1: world.getMap().getOppRespawnZone())
                tmp += world.manhattanDistance(cell, cell1);
            if (tmp/4 < minDist){
                chosenObjectiveCloseToThem = cell;
                minDist = tmp/4;
            }
        }

//        System.out.println("chosen of us: " + chosenObjectiveCloseToUs.getRow() + " " + chosenObjectiveCloseToUs.getColumn());
//        System.out.println("chosen of them: " + chosenObjectiveCloseToThem.getRow() + " " + chosenObjectiveCloseToThem.getColumn());

        enemyActionPoint = world.getMaxAP();

        long t2 = System.currentTimeMillis();
        //logger.debug("elapsed time: " + (t2 - t1));
    }

    public void pickTurn(World world)
    {
        //logger.debug("\n\n----------------      pickTurn      ----------------\n\n");
        long t1 = System.currentTimeMillis();

//        HeroName[] heroNames = new HeroName[]{HeroName.GUARDIAN, HeroName.HEALER, HeroName.GUARDIAN, HeroName.HEALER};
//
//        world.pickHero(heroNames[world.getCurrentTurn()]);

        HeroName[] ourHero;
        ourHero = new HeroName[]{HeroName.GUARDIAN, HeroName.HEALER, HeroName.HEALER, HeroName.GUARDIAN};
        System.out.println("pick started");

        world.pickHero(ourHero[world.getCurrentTurn()]);

        long t2 = System.currentTimeMillis();
        //logger.debug("elapsed time: " + (t2 - t1));
    }

    public void moveTurn(World world)
    {
        //logger.debug("\n\n----------------      moveTurn: " + world.getCurrentTurn() + " phase: " + world.getMovePhaseNum() +"      ----------------\n\n");
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

        boolean healerMove = false;
        boolean gaurdianmove = false;

        Hero[] myHeros, oppHeros;
        myHeros = world.getMyHeroes();
        oppHeros = world.getOppHeroes();


        //initial path
        Direction[][] path;
        path = new Direction[myHeros.length][];

        // find the target
        if (lastPhaseChoosedATarget != world.getCurrentTurn()){
            targetHero = findOneTarget(world);
//            System.out.println("t " + world.getCurrentTurn() + " " + targetHero);
            lastPhaseChoosedATarget = world.getCurrentTurn();
        }

        Cell[] blockedCell,blockedCell1;
        blockedCell = new Cell[100];
        blockedCell1 = new Cell[4];
        for (int i =0 ; i<myHeros.length;i++)
            blockedCell[i]=myHeros[i].getCurrentCell();

        int dist1t,dist2t,dist3t,dist0t,dist02,dist13;

        //move
        if (targetHero != null && targetHero.getCurrentCell().getRow() != -1){
            dist0t=world.manhattanDistance(myHeros[0].getCurrentCell(),targetHero.getCurrentCell());
            dist2t=world.manhattanDistance(myHeros[2].getCurrentCell(),targetHero.getCurrentCell());
            dist1t=world.manhattanDistance(myHeros[1].getCurrentCell(),targetHero.getCurrentCell());
            dist3t=world.manhattanDistance(myHeros[3].getCurrentCell(),targetHero.getCurrentCell());
            if (dist0t < dist1t) {
                path[0] = world.getPathMoveDirections(myHeros[0].getCurrentCell(), targetHero.getCurrentCell());
                if (path[0].length != 0 && dist0t>1){
                    int curDistance =  world.manhattanDistance(myHeros[3].getCurrentCell(), myHeros[0].getCurrentCell());
                    if (curDistance < 500)
                        world.moveHero(myHeros[0], path[0][0]);
//                    else {
//                        int newDistance = 0;
//                        int row1 = myHeros[0].getCurrentCell().getRow();
//                        int col1 = myHeros[0].getCurrentCell().getColumn();
//                        int row2 = myHeros[3].getCurrentCell().getRow();
//                        int col2 = myHeros[3].getCurrentCell().getColumn();
//                        if (path[0][0] == Direction.UP)
//                            newDistance = world.manhattanDistance(row1 - 1, col1, row2, col2);
//                        else if (path[0][0] == Direction.RIGHT)
//                            newDistance = world.manhattanDistance(row1, col1 + 1, row2, col2);
//                        else if (path[0][0] == Direction.DOWN)
//                            newDistance = world.manhattanDistance(row1 + 1, col1, row2, col2);
//                        else
//                            newDistance = world.manhattanDistance(row1, col1 - 1, row2, col2);
//
//                        if (newDistance < curDistance)
//                            world.moveHero(myHeros[0], path[0][0]);
//                        else {
//                            path[0] = world.getPathMoveDirections(myHeros[0].getCurrentCell(), myHeros[3].getCurrentCell());
//                        }
//                    }
                }
                path[1] = world.getPathMoveDirections(myHeros[1].getCurrentCell(), myHeros[0].getCurrentCell());
                if (path[1].length != 0 && world.manhattanDistance(myHeros[1].getCurrentCell(), myHeros[0].getCurrentCell()) >4)
                    world.moveHero(myHeros[1], path[1][0]);
            }
            else if(dist0t >= dist1t ) {
                path[0] = world.getPathMoveDirections(myHeros[0].getCurrentCell(), targetHero.getCurrentCell(),blockedCell);
                if (path[0].length != 0)
                    world.moveHero(myHeros[0], path[0][0]);
                path[1] = world.getPathMoveDirections(myHeros[1].getCurrentCell(), chosenObjectiveCloseToUs);
                if (path[1].length != 0)
                    world.moveHero(myHeros[1], path[1][0]);
            }

            //couple 3,2
            if (dist3t < dist2t) {
                path[3] = world.getPathMoveDirections(myHeros[3].getCurrentCell(), targetHero.getCurrentCell());
                if (path[3].length != 0 && dist3t>1){
                    int curDistance = world.manhattanDistance(myHeros[3].getCurrentCell(), myHeros[0].getCurrentCell());
                    if (curDistance < 500)
                        world.moveHero(myHeros[3], path[3][0]);
//                    else {
//                        int newDistance = 0;
//                        int row1 = myHeros[3].getCurrentCell().getRow();
//                        int col1 = myHeros[3].getCurrentCell().getColumn();
//                        int row2 = myHeros[0].getCurrentCell().getRow();
//                        int col2 = myHeros[0].getCurrentCell().getColumn();
//                        if (path[3][0] == Direction.UP)
//                            newDistance = world.manhattanDistance(row1 - 1, col1, row2, col2);
//                        else if (path[3][0] == Direction.RIGHT)
//                            newDistance = world.manhattanDistance(row1, col1 + 1, row2, col2);
//                        else if (path[3][0] == Direction.DOWN)
//                            newDistance = world.manhattanDistance(row1 + 1, col1, row2, col2);
//                        else
//                            newDistance = world.manhattanDistance(row1, col1 - 1, row2, col2);
//
//                        if (newDistance < curDistance)
//                            world.moveHero(myHeros[3], path[3][0]);
//                    }
                }

                path[2] = world.getPathMoveDirections(myHeros[2].getCurrentCell(), myHeros[3].getCurrentCell());
                if (path[2].length != 0 && world.manhattanDistance(myHeros[2].getCurrentCell(), myHeros[3].getCurrentCell()) > 4)
                    world.moveHero(myHeros[2], path[2][0]);
            }
            else if(dist3t >= dist2t) {
                path[3] = world.getPathMoveDirections(myHeros[3].getCurrentCell(), targetHero.getCurrentCell(),blockedCell);
                if (path[3].length != 0)
                    world.moveHero(myHeros[3], path[3][0]);
                path[2] = world.getPathMoveDirections(myHeros[2].getCurrentCell(), chosenObjectiveCloseToUs);
                if (path[2].length != 0)
                    world.moveHero(myHeros[2], path[2][0]);
            }

        }
        else{
            double distanceToZone1 = 0;
            double distanceToZone2 = 0;
            for (Hero hero: world.getMyHeroes()){
                int tmp = world.manhattanDistance(hero.getCurrentCell(), chosenObjectiveCloseToUs);
                distanceToZone1 += tmp;
                tmp = world.manhattanDistance(hero.getCurrentCell(), chosenObjectiveCloseToThem);
                distanceToZone2 += tmp;
            }
            distanceToZone1 = distanceToZone1/4;
            distanceToZone2 = distanceToZone2/4;
            for (int i = 0; i < myHeros.length; i++) {
                if (myHeros[i].getName() == HeroName.GUARDIAN){

                    if (distanceToZone1 < distanceToZone2)
                        path[i] = world.getPathMoveDirections(myHeros[i].getCurrentCell(), chosenObjectiveCloseToThem,blockedCell1);
                    else
                        path[i] = world.getPathMoveDirections(myHeros[i].getCurrentCell(), chosenObjectiveCloseToThem,blockedCell1);
                    if (path[i].length != 0)
                        world.moveHero(myHeros[i], path[i][0]);
                }else {
                    if (i == 1){
                        path[1] = world.getPathMoveDirections(myHeros[1].getCurrentCell(), myHeros[0].getCurrentCell());
                        if (path[1].length != 0 && world.manhattanDistance(myHeros[1].getCurrentCell(), myHeros[0].getCurrentCell()) >4)
                            world.moveHero(myHeros[1], path[1][0]);
                    }else if (i == 2){
                        path[2] = world.getPathMoveDirections(myHeros[2].getCurrentCell(), myHeros[3].getCurrentCell());
                        if (path[2].length != 0 && world.manhattanDistance(myHeros[2].getCurrentCell(), myHeros[3].getCurrentCell()) > 4)
                            world.moveHero(myHeros[2], path[2][0]);
                    }
                }
            }
        }

        long t2 = System.currentTimeMillis();
        //logger.debug("elapsed time: " + (t2 - t1));
    }

    private Hero findOneTarget(World world) {
        double minDist = Double.MAX_VALUE;
        double curDist;
        Hero tmpTarget = null;
        List<Hero> inZone = new ArrayList<>();
        List<Hero> outOfZone = new ArrayList<>();
        for (Hero hero: world.getOppHeroes())
            if (hero.getCurrentCell().isInObjectiveZone())
                inZone.add(hero);
            else
                outOfZone.add(hero);
        for (Hero hero: inZone){
            curDist = (world.manhattanDistance(world.getMyHeroes()[0].getCurrentCell(), hero.getCurrentCell())
                        + world.manhattanDistance(world.getMyHeroes()[3].getCurrentCell(), hero.getCurrentCell()))/2;
            if (curDist < minDist) {
                minDist = curDist;
                tmpTarget = hero;
            }
        }
        if (tmpTarget != null)
            return tmpTarget;

        minDist = Double.MAX_VALUE;
        for (Hero hero: outOfZone){
            curDist = (world.manhattanDistance(world.getMyHeroes()[0].getCurrentCell(), hero.getCurrentCell())
                    + world.manhattanDistance(world.getMyHeroes()[3].getCurrentCell(), hero.getCurrentCell()))/2;

            int distanceToZone = Integer.MAX_VALUE;
            for (Cell cell: world.getMap().getObjectiveZone()){
                int tmpint = world.manhattanDistance(cell, hero.getCurrentCell());
                if (tmpint < distanceToZone){
                    distanceToZone = tmpint;
                }
            }

            if (curDist < minDist && distanceToZone < 4) {
                minDist = curDist;
                tmpTarget = hero;
            }
        }

        return tmpTarget;
    }

    public void actionTurn(World world) {
        //logger.debug("\n\n----------------      actionTurn: " + world.getCurrentTurn() + "      ----------------\n\n");
        long t1 = System.currentTimeMillis();

//        System.out.println("action started");
        //logger.debug("remaining AP of us: " + world.getAP());
        //logger.debug("remaining AP of enemy: " + enemyActionPoint);
        Hero[] allyHeroes = world.getMyHeroes();
        //logger.debug("\nstate of our heroes:");
        for (Hero hero: allyHeroes)
            //logger.debug(hero);
        //logger.debug("\nstate of enemy heroes:");
        for (Integer heroID: coolDownMap.keySet()){
            //logger.debug("hero: " + heroID);
            for (AbilityName abilityName: coolDownMap.get(heroID).keySet()){
                int currentCoolDown = coolDownMap.get(heroID).get(abilityName);
                //logger.debug("  ability: " + abilityName + " coolDown: " + currentCoolDown);
            }
        }
        Hero[] enemyHeroes = world.getOppHeroes();
        //logger.debug("\nposition & HP of allies: ");
        for (Hero hero: allyHeroes) {
            //logger.debug("hero: " + hero.getName() + " id: " + hero.getId() + " row: " + hero.getCurrentCell().getRow() + " col: " + hero.getCurrentCell().getColumn() + " current HP: " + hero.getCurrentHP() + " max HP: " + hero.getMaxHP());
        }
        //logger.debug("\nposition & HP of enemies: ");
        for (Hero hero: enemyHeroes) {
            //logger.debug("hero: " + hero.getName() + " id: " + hero.getId() + " row: " + hero.getCurrentCell().getRow() + " col: " + hero.getCurrentCell().getColumn() + " current HP: " + hero.getCurrentHP() + " max HP: " + hero.getMaxHP());
        }
        List<Pair<Hero, Ability>[]> setsOfActions = new ArrayList<>();
        List<Pair<Hero, Ability>[]> setsOfActions_Enemy = new ArrayList<>();
        Ability[] abilities = new Ability[4];

        // check all the possible set of actions
        getAllPossibleActions(setsOfActions, allyHeroes, 0, world.getAP(), false, abilities, world.getAP());
//        int i = 1;
//        for (Pair<Hero, Ability>[] pairs: setsOfActions){
//            //logger.debug("\nset: " + i);
//            int tmpAP = 0;
//            for (Pair<Hero, Ability> pair: pairs) {
//                //logger.debug("hero: " + pair.getFirst().getId() + " ability: " + (pair.getSecond() != null ? pair.getSecond().getName() : null));
//                if (pair.getSecond() != null)
//                    tmpAP += pair.getSecond().getAPCost();
//            }
//            //logger.debug("remaining ap: " + (world.getAP() - tmpAP));
//            i++;
//        }

        getAllPossibleActions(setsOfActions_Enemy, enemyHeroes, 0, enemyActionPoint, true, abilities, enemyActionPoint);
//        int i = 1;
//        for (Pair<Hero, Ability>[] pairs: setsOfActions_Enemy){
//            //logger.debug("\nset: " + i);
//            int tmpAP = 0;
//            for (Pair<Hero, Ability> pair: pairs) {
//                //logger.debug("hero: " + pair.getFirst().getId() + " ability: " + (pair.getSecond() != null ? pair.getSecond().getName() : null));
//                if (pair.getSecond() != null)
//                    tmpAP += pair.getSecond().getAPCost();
//            }
//            //logger.debug("remaining ap: " + (enemy_AP - tmpAP));
//            i++;
//        }

        //logger.debug("\nnumber of action sets of ally: " + setsOfActions.size());
        //logger.debug("number of action sets of enemy: " + setsOfActions_Enemy.size());

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
            java.util.Map<Integer, Double> estimatedDamageToEnemy = ed_toEnemy.get(i);
            boolean didItOnce = false;
            List<Pair<Hero, Pair<Ability, Cell>>> worst = null;
            for (Pair<Hero, Ability>[] setOfEnemyActions: setsOfActions_Enemy) {
                // TODO: check the correctness of indexes
                java.util.Map<Integer, Double> estimatedDamageToAlly = ed_toAlly.get(j);
                if (!didItOnce) {
//                    //logger.debug("\n\nchoose cell for me");
                    allyAction = getBestTargetCells(world, setOfAllyActions, estimatedDamageToAlly, allyHeroes, enemyHeroes, true);
//                    for (Pair<Hero, Pair<Ability, Cell>> pair: allyAction){
//                        //logger.debug("\nhero: " + pair.getFirst());
//                        //logger.debug("ability: " + pair.getSecond().getFirst().getName() + " row: " + pair.getSecond().getSecond().getRow() + " col: " + pair.getSecond().getSecond().getColumn());
//                    }
                    didItOnce = true;
                }
//                //logger.debug("choose cell for enemy");
                List<Pair<Hero, Pair<Ability, Cell>>> enemyAction = getBestTargetCells(world, setOfEnemyActions, estimatedDamageToEnemy, enemyHeroes, allyHeroes, false);
                double score = getScore(world, allyAction, enemyAction);
//                //logger.debug("------ score: " + score);
                if (score < minScore) {
                    minScore = score;
                    worst = enemyAction;
                }
                j++;
            }

            // assign the worst probable score that can be gained by this set of actions to it self
            if (minScore > maxScore){
                bestAction = allyAction;
                maxScore = minScore;
            }

//            if (world.getCurrentTurn() > 1100){
//                //logger.debug("\nscore of this bitch: " + minScore);
//                for (Pair<Hero, Pair<Ability, Cell>> action :allyAction)
//                    //logger.debug("c: " + action.getFirst().getId() + " a: " + action.getSecond().getFirst().getName() + " c: " + action.getSecond().getSecond().getRow() + ", " + action.getSecond().getSecond().getColumn());
//                //logger.debug("reason: ");
//                for (Pair<Hero, Pair<Ability, Cell>> action :worst)
//                    //logger.debug("c: " + action.getFirst().getId() + " a: " + action.getSecond().getFirst().getName() + " c: " + action.getSecond().getSecond().getRow() + ", " + action.getSecond().getSecond().getColumn());
//            }

            if (System.currentTimeMillis() - t1 > 700){
                // send the actions to server
                for (Pair<Hero, Pair<Ability, Cell>> pair: bestAction)
                    world.castAbility(pair.getFirst(), pair.getSecond().getFirst(), pair.getSecond().getSecond().getRow(), pair.getSecond().getSecond().getColumn());
                return;
            }

            minScore = Double.MAX_VALUE;
            i++;
            j = 0;
        }

        //logger.debug("\n\n\nmaxScore: " + maxScore);
//        if (bestAction != null) {
//            for (Pair<Hero, Pair<Ability, Cell>> p: bestAction)
//                //logger.debug("hero: " + p.getFirst().getName() + " id: " + p.getFirst().getId()  + " ability: " + p.getSecond().getFirst().getName() + " cell: row: "
//                        + p.getSecond().getSecond().getRow() + " col: " + p.getSecond().getSecond().getColumn());
//        }

        // send the actions to server
        for (Pair<Hero, Pair<Ability, Cell>> pair: bestAction)
            world.castAbility(pair.getFirst(), pair.getSecond().getFirst(), pair.getSecond().getSecond().getRow(), pair.getSecond().getSecond().getColumn());

        shouldUpdateCoolDowns = true;

        long t2 = System.currentTimeMillis();
        //logger.debug("elapsed time: " + (t2 - t1));
    }

    private double getScore(World world, List<Pair<Hero, Pair<Ability, Cell>>> allyAction, List<Pair<Hero, Pair<Ability, Cell>>> enemyAction) {

        java.util.Map<Integer, Double> damageToEnemy = new HashMap<>();
        java.util.Map<Integer, Double> damageToAlly = new HashMap<>();
        double consumedAP = 0;

        for (Hero hero: world.getOppHeroes())
            damageToEnemy.put(hero.getId(), 0.0);
        for (Hero hero: world.getMyHeroes())
            damageToAlly.put(hero.getId(), 0.0);

        // consider the effect of damages
        for (Pair<Hero, Pair<Ability, Cell>> actionPair: enemyAction){
            AbilityName an = actionPair.getSecond().getFirst().getName();
            if (actionPair.getSecond().getFirst().getAreaOfEffect() != 0 && actionPair.getSecond().getFirst().getType() == AbilityType.OFFENSIVE){
                for (Hero hero: world.getMyHeroes()){
                    if (world.manhattanDistance(hero.getCurrentCell(), actionPair.getSecond().getSecond()) <= actionPair.getSecond().getFirst().getAreaOfEffect()){
                        double currentDgm = damageToAlly.get(hero.getId());
                        damageToAlly.replace(hero.getId(), currentDgm + actionPair.getSecond().getFirst().getPower());
                    }
                }
            }else if (actionPair.getSecond().getFirst().getType() == AbilityType.OFFENSIVE){
                int targetId = world.getMyHero(actionPair.getSecond().getSecond()).getId();
                double currentDgm = damageToAlly.get(targetId);
                damageToAlly.replace(targetId, currentDgm + actionPair.getSecond().getFirst().getPower());
            }
        }
        // consider the effect of damages
        for (Pair<Hero, Pair<Ability, Cell>> actionPair: allyAction){
            consumedAP += actionPair.getSecond().getFirst().getAPCost();
            if (actionPair.getSecond().getFirst().getAreaOfEffect() != 0 && actionPair.getSecond().getFirst().getType() == AbilityType.OFFENSIVE){
                for (Hero hero: world.getOppHeroes()){
                    if (world.manhattanDistance(hero.getCurrentCell(), actionPair.getSecond().getSecond()) <= actionPair.getSecond().getFirst().getAreaOfEffect()){
                        double currentDgm = damageToEnemy.get(hero.getId());
                        damageToEnemy.replace(hero.getId(), currentDgm + 2*actionPair.getSecond().getFirst().getPower());
                    }
                }
            }else if (actionPair.getSecond().getFirst().getType() == AbilityType.OFFENSIVE){
                int targetId = world.getOppHero(actionPair.getSecond().getSecond()).getId();
                double currentDgm = damageToEnemy.get(targetId);
                damageToEnemy.replace(targetId, currentDgm + actionPair.getSecond().getFirst().getPower());
            }
        }
//        //logger.debug("actions of us:");
//        for (Pair<Hero, Pair<Ability, Cell>> actionPair: allyAction){
//            //logger.debug("hero " + actionPair.getFirst().getId() + " ability " + actionPair.getSecond().getFirst().getName() + " cell " + actionPair.getSecond().getSecond().getRow() + ", " + actionPair.getSecond().getSecond().getColumn());
//        }
//        //logger.debug("dmg to them");
//        for (Integer integer: damageToEnemy.keySet())
//            //logger.debug("id " + integer + " dmg " + damageToEnemy.get(integer));
//        //logger.debug("actions of them:");
//        for (Pair<Hero, Pair<Ability, Cell>> actionPair: enemyAction){
//            if (actionPair.getSecond().getSecond() != null)
//                //logger.debug("hero " + actionPair.getFirst().getId() + " ability " + actionPair.getSecond().getFirst().getName() + " cell " + actionPair.getSecond().getSecond().getRow() + ", " + actionPair.getSecond().getSecond().getColumn());
//        }
//        //logger.debug("dmg to us");
//        for (Integer integer: damageToAlly.keySet())
//            //logger.debug("id " + integer + " dmg " + damageToAlly.get(integer));

        // consider the effect of healing and fortifying
        for (Pair<Hero, Pair<Ability, Cell>> actionPair: enemyAction){
            AbilityName an = actionPair.getSecond().getFirst().getName();
            if (an == AbilityName.GUARDIAN_FORTIFY){
                Hero targetHero = world.getOppHero(actionPair.getSecond().getSecond());
                // fortified hero takes no damage
                double oldDamage = damageToEnemy.get(targetHero.getId());
                damageToEnemy.replace(targetHero.getId(), oldDamage/3);
            }else if (an == AbilityName.HEALER_HEAL){
                Hero targetHero = world.getOppHero(actionPair.getSecond().getSecond());
                // reduce the damage equal to healing power
                double oldDamage = damageToEnemy.get(targetHero.getId());
                // heal effect can not be more than reduced HP
                int maxHealPower = Integer.min(targetHero.getMaxHP() - targetHero.getCurrentHP(), actionPair.getSecond().getFirst().getPower());
                double newDamage = oldDamage - maxHealPower;
                damageToEnemy.replace(targetHero.getId(), newDamage);
            }else if (an == AbilityName.BLASTER_DODGE || an == AbilityName.GUARDIAN_DODGE || an == AbilityName.HEALER_DODGE || an == AbilityName.SENTRY_DODGE){
                // assume that hero could ran away
                double oldDamage = damageToEnemy.get(actionPair.getFirst().getId());
                damageToEnemy.replace(actionPair.getFirst().getId(), oldDamage/2);
            }
        }
        // consider the effect of healing and fortifying
        for (Pair<Hero, Pair<Ability, Cell>> actionPair: allyAction){
            AbilityName an = actionPair.getSecond().getFirst().getName();
            if (an == AbilityName.GUARDIAN_FORTIFY){
                Hero targetHero = world.getMyHero(actionPair.getSecond().getSecond());
                double oldDamage = damageToAlly.get(targetHero.getId());
                // fortified hero takes no damage
                damageToAlly.replace(targetHero.getId(), oldDamage/3);
            }else if (an == AbilityName.HEALER_HEAL){
                Hero targetHero = world.getMyHero(actionPair.getSecond().getSecond());
                // reduce the damage equal to healing power
                double oldDamage = damageToAlly.get(targetHero.getId());
                // heal effect can not be more than reduced HP
                double maxHealPower = Integer.min(targetHero.getMaxHP() - targetHero.getCurrentHP(), actionPair.getSecond().getFirst().getPower());
                if (maxHealPower < 10)
                    maxHealPower = -actionPair.getSecond().getFirst().getAPCost()*1.2;
                else
                    maxHealPower = maxHealPower*2;
                double newDamage = oldDamage - maxHealPower;
                damageToAlly.replace(targetHero.getId(), newDamage);
            }else if (an == AbilityName.BLASTER_DODGE || an == AbilityName.GUARDIAN_DODGE || an == AbilityName.HEALER_DODGE || an == AbilityName.SENTRY_DODGE){
                // assume that hero could ran away
                double oldDamage = damageToAlly.get(actionPair.getFirst().getId());
                damageToAlly.replace(actionPair.getFirst().getId(), oldDamage/2);
            }
        }

//        //logger.debug("final dmg to them");
//        for (Integer integer: damageToEnemy.keySet())
//            //logger.debug("id " + integer + " dmg " + damageToEnemy.get(integer));
//        //logger.debug("final dmg to us");
//        for (Integer integer: damageToAlly.keySet())
//            //logger.debug("id " + integer + " dmg " + damageToAlly.get(integer));

//        double totalDamageToEnemy2 = 0.0, totalDamageToAlly2 = 0.0;
//        for (Double d: estimatedDamageToAlly.values())
//            totalDamageToAlly2 += d;
//        for (Double d: estimatedDamageToEnemy.values())
//            totalDamageToEnemy2 += d;
//        //logger.debug("\nbefore heal -> to us: " + totalDamageToAlly2 + " to them: " + totalDamageToEnemy2);

        double totalDamageToEnemy = 0.0, totalDamageToAlly = 0.0;
        for (Double d: damageToAlly.values())
            totalDamageToAlly += d;
        for (Double d: damageToEnemy.values())
            totalDamageToEnemy += d;
//        //logger.debug("after heal -> to us: " + totalDamageToAlly + " to them: " + totalDamageToEnemy);

//        //logger.debug("score: " + (totalDamageToEnemy - totalDamageToAlly));

        int someNum = 0;
        for (Pair<Hero, Pair<Ability, Cell>> actionPair: allyAction){
            if (actionPair.getSecond().getFirst().getName() == AbilityName.GUARDIAN_ATTACK)
                someNum ++;
        }
        double score = 1.2*totalDamageToEnemy - totalDamageToAlly + consumedAP;

//        if (someNum!=0)
//            //logger.debug("score of guardians " + score + " #" + someNum);

        return score;
    }

    private Cell bestPlaceForHealerDodge(World world, Hero myHero, List<Cell> ignoreForDodging, int range){

        double[][] distanceFromEnemy = new double[world.getMap().getRowNum()][world.getMap().getColumnNum()];
        int[][] numberOfAliesinRangeToHeal = new int[world.getMap().getRowNum()][world.getMap().getColumnNum()];
        Ability ability = myHero.getAbility(AbilityName.HEALER_DODGE);

        for (Cell cell: ignoreForDodging)
            numberOfAliesinRangeToHeal[cell.getRow()][cell.getColumn()] = -9999;

        for (Hero aly:world.getMyHeroes()) {
            if (myHero.equals(aly))
                continue;
            Cell alyCell = aly.getCurrentCell();
            int alyRow = alyCell.getRow();
            int alyCol = alyCell.getColumn();
            for (int i = -ability.getAreaOfEffect(); i<= ability.getAreaOfEffect(); i++){
                for (int j = -ability.getAreaOfEffect(); j<= ability.getAreaOfEffect(); j++){
                    if ((Math.abs(i)+Math.abs(j)) <= ability.getAreaOfEffect() && (alyRow + i)>=0 && (alyRow + i)<world.getMap().getRowNum() && (alyCol + j)>=0 && (alyCol + j)<world.getMap().getColumnNum()){
                        if (aly.getName() == HeroName.HEALER){
                            if (numberOfAliesinRangeToHeal[alyRow + i][alyCol + j] == 0)
                                numberOfAliesinRangeToHeal[alyRow + i][alyCol + j]+= 4;
                            else
                                numberOfAliesinRangeToHeal[alyRow + i][alyCol + j]+= 2;
                        }
                        if (aly.getName() == HeroName.GUARDIAN){
                            if (numberOfAliesinRangeToHeal[alyRow + i][alyCol + j] == 0)
                                numberOfAliesinRangeToHeal[alyRow + i][alyCol + j]+= 10;
                            else
                                numberOfAliesinRangeToHeal[alyRow + i][alyCol + j]+= 5;
                        }
                    }
                }
            }
        }

        for (Hero enemy:world.getOppHeroes()) {
            Cell enemyCell = enemy.getCurrentCell();
            int enemyRow = enemyCell.getRow();
            int enemyCol = enemyCell.getColumn();
            if (enemyRow<0 || enemyCol<0)
                continue;
            for (int i = 0 ; i < world.getMap().getRowNum() ; i++) {
                for (int j = 0; j < world.getMap().getColumnNum(); j++) {
                    double dist = Math.abs(i - enemyRow) + Math.abs(j - enemyCol);
                    if (dist >= 13) {
                        double extra = dist - 13;
                        extra /= 4;
                        dist = 13 + extra;
                    }
                    dist = 1.2 * ((Math.log(dist)/Math.log(1.3)) + 1);
                    distanceFromEnemy[i][j]+= dist;
                }
            }
        }

        // objective = distanceFromEnemy + 2*numberOfAliesinRangeToHeal + (isObjectiveZone ? 10 : 0)
        int bestI = 0;
        int bestJ = 0;
        double bestValue = 0;
        for (int i = 0; i < world.getMap().getRowNum(); i++) {
            for (int j = 0; j < world.getMap().getColumnNum(); j++) {
                double value = distanceFromEnemy[i][j] + 2*numberOfAliesinRangeToHeal[i][j];
                if (world.getMap().getCell(i, j).isInObjectiveZone())
                    value += 10;

                if (value > bestValue && (Math.abs(myHero.getCurrentCell().getRow() - i) + Math.abs(myHero.getCurrentCell().getColumn() - j) <= range)){
                    bestValue = value;
                    bestI = i;
                    bestJ = j;
                }
            }
        }
        if (bestI == 0 && bestJ == 0)
            return null;
        return world.getMap().getCell(bestI, bestJ);

    }

    private List<Pair<Hero, Pair<Ability, Cell>>> getBestTargetCells(World world, Pair<Hero, Ability>[] setOfActions, java.util.Map<Integer, Double> estimatedDamageToUs, Hero[] allyHeroes, Hero[] enemyHeroes, boolean chooseForAlly) {
        List<Pair<Hero, Pair<Ability, Cell>>> actionsAndTargets = new ArrayList<>();
        List<Cell> ignoreForDodging = new ArrayList<>();
        List<Cell> ignoreForFortifying = new ArrayList<>();
        for (Hero ally: world.getMyHeroes()) {
            if (ally.getCurrentCell().getRow() != -1)
                ignoreForDodging.add(ally.getCurrentCell());
        }
        for (Pair<Hero, Ability> actionPair: setOfActions){
            if (actionPair.getSecond() == null)
                continue;
//            //logger.debug("\nchoose a target for: " + actionPair.getFirst());
            Cell targetCell = null;
            AbilityName an = actionPair.getSecond().getName();
            if (an == AbilityName.HEALER_ATTACK || an == AbilityName.SENTRY_ATTACK){
                List<Hero> inRangeHeroes = getInTargetableHeroes(actionPair.getFirst(), actionPair.getSecond(), enemyHeroes, false);
                inRangeHeroes.sort(new Comparator<Hero>() {
                    @Override
                    public int compare(Hero o1, Hero o2) {
                        return o1.getCurrentHP()/o1.getMaxHP() - o2.getCurrentHP()/o2.getMaxHP();
                    }
                });
                if (inRangeHeroes.size() != 0) {
                    targetCell = inRangeHeroes.get(0).getCurrentCell();
//                    //logger.debug("lowest hp: " + inRangeHeroes.get(0));
                    Pair<Ability, Cell> pair1 = new Pair<>(actionPair.getSecond(), targetCell);
                    Pair<Hero, Pair<Ability, Cell>> pair2 = new Pair<>(actionPair.getFirst(), pair1);
                    actionsAndTargets.add(pair2);
                }
            }else if (an == AbilityName.BLASTER_DODGE || an == AbilityName.GUARDIAN_DODGE || an == AbilityName.HEALER_DODGE || an == AbilityName.SENTRY_DODGE){
                if (chooseForAlly) {
                    if (actionPair.getFirst().getName() == HeroName.HEALER)
                        targetCell = bestPlaceForHealerDodge(world, actionPair.getFirst(), ignoreForDodging, actionPair.getSecond().getRange());
                    else
                        targetCell = whereToDodge(world, actionPair.getFirst(), ignoreForDodging, actionPair.getSecond().getRange(), getLowestHpTarget(world));
                    if (targetCell != null && targetCell.getRow() != -1 && targetCell.getColumn() != -1) {
                        ignoreForDodging.add(targetCell);
                        ignoreForDodging.remove(actionPair.getFirst().getCurrentCell());
                        Pair<Ability, Cell> pair1 = new Pair<>(actionPair.getSecond(), targetCell);
                        Pair<Hero, Pair<Ability, Cell>> pair2 = new Pair<>(actionPair.getFirst(), pair1);
                        actionsAndTargets.add(pair2);
                    }
                }else {
                    Pair<Ability, Cell> pair1 = new Pair<>(actionPair.getSecond(), null);
                    Pair<Hero, Pair<Ability, Cell>> pair2 = new Pair<>(actionPair.getFirst(), pair1);
                    actionsAndTargets.add(pair2);
                }
            }else if (an == AbilityName.BLASTER_BOMB || an == AbilityName.BLASTER_ATTACK || an == AbilityName.GUARDIAN_ATTACK){
                targetCell = bestPlaceForBomb(world, actionPair.getFirst(), actionPair.getSecond(), enemyHeroes, false);
                if (targetCell != null) {
                    Pair<Ability, Cell> pair1 = new Pair<>(actionPair.getSecond(), targetCell);
                    Pair<Hero, Pair<Ability, Cell>> pair2 = new Pair<>(actionPair.getFirst(), pair1);
                    actionsAndTargets.add(pair2);
                }
            }else if (an == AbilityName.GUARDIAN_FORTIFY){
                targetCell = whoToFortify(actionPair.getFirst(), actionPair.getSecond(), allyHeroes, estimatedDamageToUs, ignoreForFortifying);
                if (targetCell != null) {
                    Pair<Ability, Cell> pair1 = new Pair<>(actionPair.getSecond(), targetCell);
                    Pair<Hero, Pair<Ability, Cell>> pair2 = new Pair<>(actionPair.getFirst(), pair1);
                    actionsAndTargets.add(pair2);
                    ignoreForFortifying.add(targetCell);
                }
            }else if (an == AbilityName.HEALER_HEAL){
                targetCell = whoToHeal(actionPair.getFirst(), actionPair.getSecond(), allyHeroes, estimatedDamageToUs, ignoreForFortifying);
                if (targetCell != null) {
                    Pair<Ability, Cell> pair1 = new Pair<>(actionPair.getSecond(), targetCell);
                    Pair<Hero, Pair<Ability, Cell>> pair2 = new Pair<>(actionPair.getFirst(), pair1);
                    actionsAndTargets.add(pair2);
                    ignoreForFortifying.add(targetCell);
                }
            }else if (an == AbilityName.SENTRY_RAY){
                List<Hero> inRangeHeroes = getInTargetableHeroes(actionPair.getFirst(), actionPair.getSecond(), enemyHeroes, false);
                if (inRangeHeroes.size() != 0) {
                    targetCell = inRangeHeroes.get(0).getCurrentCell();
                    Pair<Ability, Cell> pair1 = new Pair<>(actionPair.getSecond(), targetCell);
                    Pair<Hero, Pair<Ability, Cell>> pair2 = new Pair<>(actionPair.getFirst(), pair1);
                    actionsAndTargets.add(pair2);
                }
            }
//            //logger.debug("use " + actionPair.getSecond().getName() + " on row: " + (targetCell != null ? targetCell.getRow() : null) + " col: " + (targetCell != null ? targetCell.getColumn() : null));
        }

        return actionsAndTargets;
    }

    private Hero getLowestHpTarget(World world) {
        List<Hero> list = new ArrayList<>();
        for (Hero hero: world.getOppHeroes())
            list.add(hero);
        list.sort(new Comparator<Hero>() {
            @Override
            public int compare(Hero o1, Hero o2) {
                return o1.getCurrentHP()/o1.getMaxHP() - o2.getCurrentHP()/o2.getMaxHP();
            }
        });
        return list.get(0);
    }

    private Cell whoToHeal(Hero caster, Ability ability, Hero[] allyHeroes, java.util.Map<Integer, Double> estimatedDamageToUs, List<Cell> ignoreForFortifying) {
        double maxValue = Double.MIN_VALUE;
        Cell target = null;
        for (Hero hero: allyHeroes){
            boolean ignore = false;
            for (Cell ignoreCell: ignoreForFortifying)
                if (ignoreCell.getColumn() == hero.getCurrentCell().getColumn() && ignoreCell.getRow() == hero.getCurrentCell().getRow())
                    ignore = true;
            double tmpDmg = estimatedDamageToUs.get(hero.getId());
            double value = tmpDmg + (hero.getMaxHP() - hero.getCurrentHP());
            if (!ignore && value > maxValue && (Math.abs(hero.getCurrentCell().getRow() - caster.getCurrentCell().getRow()) + Math.abs(hero.getCurrentCell().getColumn() - caster.getCurrentCell().getColumn()) < ability.getRange())) {
                maxValue = value;
                target = hero.getCurrentCell();
            }
        }
        return target;
    }

    private Cell bestPlaceForBomb(World world, Hero myHero, Ability ability, Hero[] enemies, boolean debug){
        ArrayList<Hero> inRangeEnemies = getInTargetableHeroes(myHero, ability, enemies, debug);

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
                if (map[i][j]> bestValue && (Math.abs(i - myHero.getCurrentCell().getRow()) + Math.abs((j - myHero.getCurrentCell().getColumn())) <= ability.getRange())){
                    bestValue = map[i][j];
                    bestI = i;
                    bestJ = j;
                }
            }
        }
        if (bestI == 0 && bestJ == 0)
            return null;
        return world.getMap().getCell(bestI, bestJ);
    }

    private Cell whoToFortify(Hero caster, Ability ability, Hero[] allyHeroes, java.util.Map<Integer, Double> estimatedDamageToUs, List<Cell> ignoreForFortifying) {
        double maxDmg = Double.MIN_VALUE;
        Cell target = null;
        for (Hero hero: allyHeroes){
            boolean ignore = false;
            for (Cell ignoreCell: ignoreForFortifying)
                if (ignoreCell.getColumn() == hero.getCurrentCell().getColumn() && ignoreCell.getRow() == hero.getCurrentCell().getRow())
                    ignore = true;
            double tmpDmg = estimatedDamageToUs.get(hero.getId());
            if (!ignore && tmpDmg > maxDmg && (Math.abs(hero.getCurrentCell().getRow() - caster.getCurrentCell().getRow()) + Math.abs(hero.getCurrentCell().getColumn() - caster.getCurrentCell().getColumn()) < ability.getRange())) {
                maxDmg = tmpDmg;
                target = hero.getCurrentCell();
            }
        }
        return target;
    }

    //TODO: check the range (make it recursive to find a close non blocked one)
    private Cell whereToDodge(World world, Hero hero, List<Cell> ignoreForDodging, int range, Hero target) {
        if (target.getCurrentCell().getRow() == -1 || target.getCurrentCell().getColumn() == -1)
            return null;
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                Cell cell = world.getMap().getCell(target.getCurrentCell().getRow() + i, target.getCurrentCell().getColumn() + j);
                boolean shouldBeIgnored = false;
                for (Cell cell2: ignoreForDodging)
                    if (cell2.getRow() == cell.getRow() && cell2.getColumn() == cell.getColumn()) {
                        shouldBeIgnored = true;
                        break;
                    }
                if (!shouldBeIgnored && (Math.abs(hero.getCurrentCell().getRow() - cell.getRow()) + Math.abs(hero.getCurrentCell().getColumn() - cell.getColumn()) < range))
                    return cell;
            }
        }

        return null;
    }

    private void totalExpectedDamage(List<java.util.Map<Integer,Double>> listOfDamages, Hero[] casters, Hero[] targets, List<Pair<Hero,Ability>[]> setsOfActions) {

        for (Pair<Hero, Ability>[] setOfActions: setsOfActions){
//            //logger.debug("action set #" + (numberOfActionSet+1) + " ----123");
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
//            //logger.debug("total damages:");
//            for (Double d: tmpDamage)
//                //logger.debug(d);
//
//            //logger.debug("result: ");
//            for (int id: tmpMap.keySet())
//                //logger.debug("id: " + id + " dmg: " + tmpMap.get(id));

        }


    }

    /**
     * find all possible set of actions based on coolDowns and remaining action point
     */
    private void getAllPossibleActions(List<Pair<Hero, Ability>[]> possibleOrders, Hero[] heroes, int heroIndex, int remainingAP, boolean isEnemyHero, Ability[] chosenAbilities, int initialAP) {

//        //logger.debug("ap: " + remainingAP + " index: " + heroIndex + " size: " + possibleOrders.size());
        int i = 0;
        for (Ability ability: heroes[heroIndex].getAbilities()){
            i++;
            if (isEnemyHero) {
                // check if ability can be cast at this time
//                //logger.debug(ability.getName() + "\n");
                int remCoolDown = coolDownMap.get(heroes[heroIndex].getId()).get(ability.getName());
//                //logger.debug("remainingAP: " + remainingAP + " ap cost " + ability.getAPCost() + " remCoolDown: " + remCoolDown + " AP - cost: " + (remainingAP - ability.getAPCost()) + " i: " + i);
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
//                //logger.debug("ap: " + remainingAP + " index: " + heroIndex + " remCoolDown: " + ability.getRemCooldown() + " AP - cost: " + (remainingAP - ability.getAPCost()) + " i: " + i);
//                //logger.debug(ability.getName() + "\n");
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

        Hero[] enemies = world.getOppHeroes();

        if (world.getMovePhaseNum() == 0)
            enemyActionPoint = 100;
        else {
            if (!((enemies[0].getCurrentCell().getRow() == lastLocationOfEnemies[0].getRow()) && (enemies[0].getCurrentCell().getColumn() == lastLocationOfEnemies[0].getColumn()) ))
                enemyActionPoint -= enemies[0].getMoveAPCost();
            if (!((enemies[1].getCurrentCell().getRow() == lastLocationOfEnemies[1].getRow()) && (enemies[1].getCurrentCell().getColumn() == lastLocationOfEnemies[1].getColumn()) ))
                enemyActionPoint -= enemies[1].getMoveAPCost();
            if (!((enemies[2].getCurrentCell().getRow() == lastLocationOfEnemies[2].getRow()) && (enemies[2].getCurrentCell().getColumn() == lastLocationOfEnemies[2].getColumn()) ))
                enemyActionPoint -= enemies[2].getMoveAPCost();
            if (!((enemies[3].getCurrentCell().getRow() == lastLocationOfEnemies[3].getRow()) && (enemies[3].getCurrentCell().getColumn() == lastLocationOfEnemies[3].getColumn()) ))
                enemyActionPoint -= enemies[3].getMoveAPCost();
        }

        Cell c0 = enemies[0].getCurrentCell();
        Cell c1 = enemies[1].getCurrentCell();
        Cell c2 = enemies[2].getCurrentCell();
        Cell c3 = enemies[3].getCurrentCell();
        lastLocationOfEnemies = new Cell[]{c0, c1, c2, c3};

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
//            //logger.debug("AbilityName: " + castAbility.getAbilityName() + " caster id: " + castAbility.getCasterId());
            int abilityCooldDown = 0;
            for (Hero hero: world.getOppHeroes())
                if (hero.getId() == castAbility.getCasterId()) {
                    abilityCooldDown = hero.getAbility(castAbility.getAbilityName()).getCooldown();
//                    //logger.debug("new coolDown: " + (abilityCooldDown - 1));
                }
            if (coolDownMap.containsKey(castAbility.getCasterId()))
                coolDownMap.get(castAbility.getCasterId()).replace(castAbility.getAbilityName(), abilityCooldDown - 1);
        }

        for (CastAbility castAbility: world.getMyCastAbilities()){
            //logger.debug("my AbilityName: " + castAbility.getAbilityName() + " caster id: " + castAbility.getCasterId());
        }

    }

    /**
     *
     */
    public Double[] expectedDamage(Hero caster, Hero[] targets, Ability ability){
        Double[] expectedDamages = new Double[targets.length];

        /** TODO: if its bomb compute the chance of getting hit then compute the expected damage
         * and probability of lower hp heroes for getting hit is more than full hp heroes
         */
        List<Hero> inRangeHeroes = getInTargetableHeroes(caster, ability, targets, false);
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

//        //logger.debug("caster: " + caster.getId() + " ability: " + ability.getName() + " power: " + ability.getPower());
//        i = 0;
//        for (Double d: expectedDamages){
//            //logger.debug("affected: " + targets[i] + " expected damage: " + expectedDamages[i]);
//            i++;
//        }

        return expectedDamages;
    }

    private ArrayList<Hero> getInTargetableHeroes(Hero myHero, Ability ability, Hero[] enemies, boolean debug){
        ArrayList<Hero> inRangeHeroes = new ArrayList<Hero>();
        int row0 = myHero.getCurrentCell().getRow();
        int col0 = myHero.getCurrentCell().getColumn();

        for (Hero enemy : enemies) {
            int row = enemy.getCurrentCell().getRow();
            int col = enemy.getCurrentCell().getColumn();
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