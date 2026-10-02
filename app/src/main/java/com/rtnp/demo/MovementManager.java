package com.rtnp.demo;

import com.rtnp.demo.core.PlacedObject;
import android.graphics.*;
import java.util.*;
import com.rtnp.demo.gpu.effect.GpuEffectSystem;
import com.rtnp.demo.logic.MovementSystem;
import com.rtnp.demo.logic.MiningSystem;
import com.rtnp.demo.logic.DockingSystem;
import com.rtnp.demo.logic.SpatialGrid;
import com.rtnp.demo.ai.AiManager;
import com.rtnp.demo.logic.MultiMoveCoordinator;
import com.rtnp.demo.ui.render.button.ProgressBarTool;
import com.rtnp.demo.logic.HexGridManager;
import com.rtnp.demo.logic.AutoLaunchTimer;
import com.rtnp.demo.logic.BuildManager;
import com.rtnp.demo.logic.UnitEffectManager;
import com.rtnp.demo.strategy.StrategyManager;
import com.rtnp.demo.strategy.StrategyTools;
import com.rtnp.demo.factory.FactoryManager;

public class MovementManager {
    private final UnitSystem host;
    private SpatialGrid spatialGrid;
    private List<PlacedObject> bulletQueryList = new ArrayList<>();

    private final List<PlacedObject> reusableList = new ArrayList<>();
    private final List<PlacedObject> newShips = new ArrayList<>();
    private final Map<PlacedObject, PlacedObject> shipsToDock = new HashMap<>();

    public MovementManager(UnitSystem host) {
        if (spatialGrid == null) {
            spatialGrid = new SpatialGrid(3000f, 5000f, 200f);
        }
        this.host = host;
    }

    public void updateMovement() {
        if (host == null) return;

        long now = System.currentTimeMillis();
        if (host.lastFrameTime == 0) { host.lastFrameTime = now; return; }
        float frameTime = (now - host.lastFrameTime) / 1000f;
        host.lastFrameTime = now;
        if (frameTime > 0.1f) frameTime = 0.1f;
        host.accumulator += frameTime;

        newShips.clear();
        shipsToDock.clear();

        while (host.accumulator >= UnitSystem.FIXED_DT) {
            HexGridManager.INSTANCE.refreshAllUnits(host.placedObjects);
            AiManager.INSTANCE.update(UnitSystem.FIXED_DT);

            reusableList.clear();
            reusableList.addAll(host.placedObjects);
            com.rtnp.demo.trait.TraitManager.INSTANCE.tick(host.placedObjects, UnitSystem.FIXED_DT);

            host.movementSystem.update(UnitSystem.FIXED_DT, host.placedObjects);
            for (PlacedObject unit : reusableList) {
                UnitEffectManager.INSTANCE.updateEffects(unit);
            }

            for (PlacedObject unit : reusableList) {
                if ("missile".equals(unit.category)) continue;
                if (unit.actionDock && !unit.isDocked) {
                    DockingSystem.INSTANCE.checkArrival(unit, host.placedObjects);
                }
            }
            DockingSystem.INSTANCE.cleanDeadFromLists(host.placedObjects);

            for (PlacedObject unit : reusableList) {
                if ("missile".equals(unit.category)) continue;
                if (unit.isUndocking) {
                    host.undockShip(unit);
                }
            }

            MultiMoveCoordinator.INSTANCE.update();

            BuildManager.INSTANCE.updateProcesses(UnitSystem.FIXED_DT);

            MiningSystem.INSTANCE.update(UnitSystem.FIXED_DT, host.placedObjects);

            com.rtnp.demo.combat.WeaponLoop.INSTANCE.update(host, UnitSystem.FIXED_DT);

            if (host.missileSystem != null) {
                host.missileSystem.update(UnitSystem.FIXED_DT);
            }

            com.rtnp.demo.combat.MissileLoop.INSTANCE.updateBullets(host, UnitSystem.FIXED_DT);

            host.physicsWorld.update(UnitSystem.FIXED_DT);

            List<PlacedObject> autoLaunched = AutoLaunchTimer.INSTANCE.update(reusableList, UnitSystem.FIXED_DT);
            for (PlacedObject unit : autoLaunched) {
                AutoLaunchTimer.INSTANCE.execute(unit);
                host.actionPending = false;
                host.actionDock = false;
            }

            com.rtnp.demo.ui.WarningMessage.INSTANCE.update(UnitSystem.FIXED_DT);

            // ★ 先移除死亡单位
            host.deathManager.removeDeadUnits();
            host.deathManager.updateLifetimes(UnitSystem.FIXED_DT);

            // ★ 然后更新策略快照，使本帧策略可以读取到本帧死亡单位
            StrategyTools tools = StrategyManager.INSTANCE.getTools();
            if (tools != null) {
                tools.updateFrame();
            }
            
            for (PlacedObject obj : reusableList) {
                if (!host.placedObjects.contains(obj)) continue;
                if (obj.factorySlotConfigs != null && !obj.factorySlotConfigs.isEmpty()) {
                    
                    FactoryManager.updateFactories(obj, UnitSystem.FIXED_DT);
                }
            }
            for (PlacedObject obj : reusableList) {
                if (!host.placedObjects.contains(obj)) continue;
                if (obj.shieldSlotConfigs != null && !obj.shieldSlotConfigs.isEmpty()) {
                    com.rtnp.demo.shield.ShieldManager.INSTANCE.updateShields(obj, UnitSystem.FIXED_DT);
                }
            }

            // ★ 最后更新策略（此时死亡单位列表已就绪）
            for (PlacedObject obj : reusableList) {
                if (!host.placedObjects.contains(obj)) continue; // 确保单位还存活
                if (obj.strategyIds != null && !obj.strategyIds.isEmpty()) {
                    StrategyManager.INSTANCE.updateStrategies(obj, UnitSystem.FIXED_DT, obj.strategyIds);
                }
                if (obj.strategyCooldownBars != null) {
                    for (int i = 0; i < obj.strategyCooldownBars.size(); i++) {
                        ProgressBarTool bar = obj.strategyCooldownBars.get(i);
                        if (bar != null && !bar.isFinished()) {
                            bar.addProgress(UnitSystem.FIXED_DT / bar.totalDuration);
                        }
                    }
                }
            }

            host.accumulator -= UnitSystem.FIXED_DT;
        }

        for (PlacedObject ns : newShips) {
            host.placedObjects.add(ns);
            host.healthMap.put(ns, (float) ns.health);
            host.physicsWorld.addUnit(ns);
        }

        for (Map.Entry<PlacedObject, PlacedObject> e : shipsToDock.entrySet()) {
            PlacedObject ship = e.getKey();
            PlacedObject base = e.getValue();
            if (host.placedObjects.contains(ship) && host.placedObjects.contains(base)) {
                DockingSystem.INSTANCE.requestDock(ship, base);
            }
        }

        long cur = System.currentTimeMillis();
        Iterator<TargetIndicator> tit = host.targetIndicators.iterator();
        while (tit.hasNext()) {
            if (cur - tit.next().startTime > 1000) {
                tit.remove();
            }
        }
    }

    public boolean isAnySelectedUnitMoving() {
        if (host == null || host.multiSelectManager == null || !host.multiSelectManager.isActive()) return false;
        for (PlacedObject unit : host.multiSelectManager.getSelectedUnits()) {
            if (unit.isMoving || unit.isStopping) return true;
        }
        return false;
    }

    public void startMultiMove(float worldX, float worldY) {
        MultiMoveCoordinator.INSTANCE.startMove(host, worldX, worldY);
    }

    public void stopSelectedUnitsMoving() {
        if (host == null || host.multiSelectManager == null || !host.multiSelectManager.isActive()) return;
        if (host.missileSystem != null) {
            try { host.missileSystem.selfDestructWatched(); } catch (Exception e) {}
        }
        Set<PlacedObject> selected = new HashSet<>(host.multiSelectManager.getSelectedUnits());
        for (PlacedObject unit : selected) {
            if (unit == null || !host.placedObjects.contains(unit) || "missile".equals(unit.category)) continue;
            if (unit.isMoving || unit.isStopping) {
                unit.isMoving = false;
                unit.isStopping = true;
                unit.targetX = unit.worldX;
                unit.targetY = unit.worldY;
                unit.dockTarget = null;
                unit.dockOffsetX = 0;
                unit.dockOffsetY = 0;
            }
        }
    }

    public boolean isMultiDockAvailable() {
        if (host == null || host.pendingMoveUnits.isEmpty()) return false;
        for (PlacedObject unit : host.pendingMoveUnits) {
            if (host.getDockTargetAt(host.multiMoveTargetX, host.multiMoveTargetY, unit) != null) return true;
        }
        return false;
    }

    public void confirmMultiMove() {
    }

    public void cancelMultiMove() {
        MultiMoveCoordinator.INSTANCE.cancel();
        host.multiActionPending = false;
        host.multiActionDock = false;
        host.pendingMoveUnits.clear();
    }

    public boolean isMultiActionPending() {
        return host != null && host.multiActionPending;
    }
}