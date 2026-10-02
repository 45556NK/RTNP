package com.rtnp.demo;

import com.rtnp.demo.core.PlacedObject;
import com.rtnp.demo.core.InventoryItem;
import com.rtnp.demo.movement.MissileSystem;
import com.rtnp.demo.logic.manager.UnitManager;
import com.rtnp.demo.logic.MovementSystem;
import com.rtnp.demo.logic.MultiSelectManager;
import com.rtnp.demo.logic.BuildManager;
import com.rtnp.demo.logic.DockingSystem;
import com.rtnp.demo.logic.SpatialGrid;
import com.rtnp.demo.logic.DeathManager;
import com.rtnp.demo.core.TemplateRegistry;
import com.rtnp.demo.trait.TraitManager;
import com.rtnp.demo.combat.Bullet;
import com.rtnp.demo.combat.CombatManager;
import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import java.util.*;
import com.rtnp.demo.strategy.StrategyManager;
import com.rtnp.demo.ui.HexButton;
import com.rtnp.demo.movement.ModuleManager;
import com.rtnp.demo.ui.RenameDialog;
import com.rtnp.demo.ai.AiManager;

import com.rtnp.demo.physics.PhysicsWorld;
import com.rtnp.demo.ui.panel.RightPanel;

public class UnitSystem {

    public Context context;
    public List<PlacedObject> placedObjects = new ArrayList<>();
    public PlacedObject selectedUnit = null;
    public boolean actionLocked = false;

    public List<Bullet> bullets = new ArrayList<>();
    public Map<PlacedObject, Long> lastAttackTime = new HashMap<>();
    public Map<PlacedObject, Float> healthMap = new HashMap<>();

    public List<TargetIndicator> targetIndicators = new ArrayList<>();
    public MissileSystem missileSystem;
    public MovementSystem movementSystem = MovementSystem.INSTANCE;

    public long lastFrameTime = 0;
    public float accumulator = 0f;
    public static float FIXED_DT = 1f / 60f;

    public int screenWidth, screenHeight;

    public boolean showDetails = false;
    public HexButton detailsExitBtn;

    public MultiSelectManager multiSelectManager;
    public boolean actionPending = false;
    public boolean actionDock = false;
    public List<PlacedObject> pendingMoveUnits = new ArrayList<>();
    public float multiMoveTargetX, multiMoveTargetY;
    public boolean multiActionPending = false;
    public boolean multiActionDock = false;

    public UnitManager unitManager;
    public DeathManager deathManager;
    public SpatialGrid spatialGrid;

    public List<PlacedObject> unitsWithLifetime = new ArrayList<>();
    public PhysicsWorld physicsWorld;

    public static final Map<String, String> attrNames = new HashMap<>();
    static {
        attrNames.put("health", "血量");
        attrNames.put("originalHealth", "初始血量");
        attrNames.put("speed", "速度");
        attrNames.put("acceleration", "加速度");
        attrNames.put("maxSpeed", "最大速度");
        attrNames.put("deceleration", "减速度");
        attrNames.put("range", "射程");
        attrNames.put("damage", "伤害");
        attrNames.put("weaponType", "武器类型");
        attrNames.put("weaponActive", "武器开关");
        attrNames.put("faction", "阵营");
        attrNames.put("category", "种类");
        attrNames.put("missileHealth", "导弹血量");
        attrNames.put("missileDamage", "导弹伤害");
        attrNames.put("missileLifetime", "导弹寿命");
        attrNames.put("explosionRange", "爆炸范围");
        attrNames.put("missileSpeed", "导弹速度");
        attrNames.put("missileAcceleration", "导弹加速度");
        attrNames.put("effectSize", "特效大小");
        attrNames.put("missileMinRange", "最小射程");
        attrNames.put("cooldown", "冷却时间");
        attrNames.put("chargeTime", "充能时间");
        attrNames.put("bulletSpeed", "子弹速度");
        attrNames.put("minDamage", "最小伤害");
        attrNames.put("maxDamage", "最大伤害");
        attrNames.put("active", "启用");
        attrNames.put("storedItem", "储存物品");
        attrNames.put("storage", "仓储");
        attrNames.put("miningSpeed", "采矿速度");
        attrNames.put("miningEnabled", "矿机状态");
        attrNames.put("collectedItems", "已采集");
        attrNames.put("消耗", "消耗资源");
        attrNames.put("生产消耗", "生产消耗");
        attrNames.put("maxService", "最大服务数");
        attrNames.put("maxDockingSlots", "最大停泊数");
        attrNames.put("dockRadius", "停泊半径");
        attrNames.put("currentSpeed", "当前速度");
        attrNames.put("heading", "朝向角度");
        attrNames.put("worldX", "世界X坐标");
        attrNames.put("worldY", "世界Y坐标");
        attrNames.put("targetX", "目标X坐标");
        attrNames.put("targetY", "目标Y坐标");
        attrNames.put("isMoving", "是否移动中");
        attrNames.put("isStopping", "是否停止中");
        attrNames.put("isDocked", "是否停泊");
        attrNames.put("isUndocking", "是否出港中");
        attrNames.put("undockProgress", "出港进度");
        attrNames.put("lifetime", "存活时间");
        attrNames.put("dockedAt", "停泊目标");
        attrNames.put("dockSlotIndex", "停泊槽位");
        attrNames.put("miningEnabled", "采矿开关");
        attrNames.put("isMining", "是否采矿中");
        attrNames.put("laserCharge", "激光充能");
        attrNames.put("missileCooldown", "导弹冷却");
        attrNames.put("weaponActive", "武器开关");
    }

    public HexButton detailsBackBtn;
    public HexButton renameBtn;

    public interface OnRedrawListener {
        void requestRedraw();
    }
    public OnRedrawListener redrawListener;
    public void requestRedraw() {
        if (redrawListener != null) redrawListener.requestRedraw();
    }

    public final MovementManager movementManager;
    public final CombatManager combatManager;
    public final SelectionManager selectionManager;

    public UnitSystem(Context context) {
        this.context = context;
        missileSystem = new MissileSystem();
        this.unitManager = new UnitManager(placedObjects, healthMap, context, this);
        TemplateRegistry.INSTANCE.init(context);

        movementManager = new MovementManager(this);
        combatManager = new CombatManager(this);
        selectionManager = new SelectionManager(this);
        this.spatialGrid = new SpatialGrid(3000f, 5000f, 200f);
        deathManager = new DeathManager(this);

        BuildManager.unitSystem = this;
        BuildManager.onCreateRealUnit = (x, y, item) -> {
            return unitManager.addObjectFromItem(item, x, y);
        };
        TraitManager.INSTANCE.setUnitSystem(this);
        physicsWorld = new PhysicsWorld();
        AiManager.INSTANCE.init(context, this);
        missileSystem.setHost(this);
        com.rtnp.demo.factory.FactoryManager.INSTANCE.init(context, this);
        com.rtnp.demo.shield.ShieldManager.INSTANCE.init(context, this);
    }

    public UnitSystem() { this(null); }

    public void addObjectFromItem(InventoryItem item, float worldX, float worldY) {
        unitManager.addObjectFromItem(item, worldX, worldY);
    }
    public List<PlacedObject> getAllUnits() { return unitManager.getAllUnits(); }
    public int getObjectCount() { return unitManager.getObjectCount(); }
    public List<PlacedObject> getFleetUnits() { return unitManager.getFleetUnits(); }

    public void handleSelectTap(float worldX, float worldY) { selectionManager.handleSelectTap(worldX, worldY); }
    public PlacedObject hitTest(float worldX, float worldY) { return selectionManager.hitTest(worldX, worldY); }
    public void setSelectedUnitDirect(PlacedObject unit) { selectionManager.setSelectedUnitDirect(unit); }
    public void setMultiSelectManager(MultiSelectManager manager) { this.multiSelectManager = manager; }
    public void deselectUnit() { selectionManager.deselectUnit(); }
    public PlacedObject getSelectedUnit() { return selectedUnit; }
    public boolean isActionLocked() { return actionLocked; }
    public boolean isShowDetails() { return showDetails; }
    public void setShowDetails(boolean show) { showDetails = show; }
    public boolean isActionPending() { return actionPending; }
    public boolean isUnitMovingOrStopping() { return selectionManager.isUnitMovingOrStopping(); }
    public boolean isDockAvailable() { return selectionManager.isDockAvailable(); }
    public void confirmAction() { selectionManager.confirmAction(); }
    public void confirmDockAction() { selectionManager.confirmDockAction(); }
    public void cancelAction() { selectionManager.cancelAction(); }
    public void stopMoving() { selectionManager.stopMoving(); }
    public void showTargetIndicator(float worldX, float worldY, int color) { selectionManager.showTargetIndicator(worldX, worldY, color); }
    public void showActionDeniedIndicator(float worldX, float worldY) { selectionManager.showActionDeniedIndicator(worldX, worldY); }
    public void setOnRedrawListener(OnRedrawListener listener) { this.redrawListener = listener; }

    public void startMultiMove(float worldX, float worldY) { movementManager.startMultiMove(worldX, worldY); }
    public void stopSelectedUnitsMoving() { movementManager.stopSelectedUnitsMoving(); }
    public boolean isMultiDockAvailable() { return movementManager.isMultiDockAvailable(); }
    public void confirmMultiMove() { movementManager.confirmMultiMove(); }
    public void cancelMultiMove() { movementManager.cancelMultiMove(); }
    public boolean isMultiActionPending() { return multiActionPending; }
    public boolean isAnySelectedUnitMoving() { return movementManager.isAnySelectedUnitMoving(); }
    public boolean hasMovingUnits() { return combatManager.hasMovingUnits(); }

    public void undockShip(PlacedObject ship) {
        DockingSystem.INSTANCE.releaseSlot(ship);
        ship.dockedAt = null;
        ship.isDocked = false;
        ship.isUndocking = false;
        ship.dockSlotIndex = -1;

        if (!ship.actionDock) {
            ship.targetX = ship.worldX;
            ship.targetY = ship.worldY;
            ship.isMoving = false;
        }

        ship.dockingTarget = null;
        if (ship == selectedUnit) actionLocked = true;
    }

    public PlacedObject getDockTargetAt(float wx, float wy, PlacedObject mover) {
        return DockingSystem.INSTANCE.findDockTarget(mover, wx, wy, placedObjects);
    }
    public static void setFixedDt(float dt) { FIXED_DT = dt; }

    public int getServiceCount(PlacedObject base) {
        int c = 0;
        for (PlacedObject u : placedObjects) if (u.dockedAt == base) c++;
        return c;
    }

    public void applyDamage(PlacedObject t, float dmg) {
        combatManager.applyDamage(t, null, null, com.rtnp.demo.combat.DamageType.ENVIRONMENT, dmg);
    }

    public void applyDamage(PlacedObject t, PlacedObject attacker,
                            PlacedObject.WeaponSlot weapon,
                            com.rtnp.demo.combat.DamageType damageType, float dmg) {
        combatManager.applyDamage(t, attacker, weapon, damageType, dmg);
    }

    public void applyDamageDirect(PlacedObject t, float dmg) {
        combatManager.applyDamageDirect(t, dmg);
    }
    public void toggleMiningEnabled() { /* 工厂系统已移除，无操作 */ }

    public PhysicsWorld getPhysicsWorld() { return physicsWorld; }
    public AiManager getAiManager() { return AiManager.INSTANCE; }

    public void updateMovement() { movementManager.updateMovement(); }

    public void updateMissileAttributesFromSlots(PlacedObject unit) {
        boolean hasMissile = false;
        for (PlacedObject.WeaponSlot ws : unit.weaponSlots) {
            if ("missile".equals(ws.type) && ws.active) {
                String typeName = ws.missileTypeName;
                if (typeName == null || typeName.isEmpty()) typeName = unit.missileTypeName;
                if (typeName == null || typeName.isEmpty()) typeName = "标准导弹";
                unit.missileTypeName = typeName;
                unit.missileCooldownMax = ws.cooldown;
                unit.missileMinRange = (int) ws.minRange;
                hasMissile = true;
                break;
            }
        }
        if (!hasMissile) {
            unit.missileTypeName = "";
            unit.missileCooldownMax = 0;
            unit.missileMinRange = 0;
        }
    }

    public void openRenameDialog() {
        if (selectedUnit == null || context == null) return;
        String curName = (selectedUnit.displayName != null) ? selectedUnit.displayName : selectedUnit.name;
        RenameDialog dialog = new RenameDialog(context, curName, placedObjects, newName -> selectedUnit.displayName = newName);
        dialog.show();
    }

    public void directSelectTap(float worldX, float worldY) {
        if (multiSelectManager != null && multiSelectManager.isActive()) return;
        if (selectedUnit != null) {
            selectedUnit.isSelected = false;
            selectedUnit = null;
        }
        actionLocked = false;
        showDetails = false;

        for (int i = placedObjects.size() - 1; i >= 0; i--) {
            PlacedObject o = placedObjects.get(i);
            if (!"unit".equals(o.type) && !"base".equals(o.type) && !"environment".equals(o.type)) continue;
            if ("missile".equals(o.category)) {
                float half = Math.max(o.size / 2f, 40f);
                if (Math.abs(worldX - o.worldX) <= half && Math.abs(worldY - o.worldY) <= half) {
                    selectedUnit = o;
                    selectedUnit.isSelected = true;
                    RightPanel.INSTANCE.lockUnit(o);
                    if (missileSystem != null) missileSystem.watch(o);
                    return;
                }
                continue;
            }
            if (isPointInObject(worldX, worldY, o)) {
                selectedUnit = o;
                selectedUnit.isSelected = true;
                RightPanel.INSTANCE.lockUnit(o);
                return;
            }
        }
        RightPanel.INSTANCE.unlock();
    }

    public void removeUnitCompletely(PlacedObject obj) {
        placedObjects.remove(obj);
        healthMap.remove(obj);
        unitsWithLifetime.remove(obj);
        if (obj == selectedUnit) deselectUnit();
        if (multiSelectManager != null) multiSelectManager.removeUnit(obj);
        if (missileSystem != null && "missile".equals(obj.category)) missileSystem.handleMissileDeath(obj);
    }

    public void applyExplosionDamage(PlacedObject missile) { combatManager.applyExplosionDamage(missile); }
    public void checkAndStartServiceForBase(PlacedObject base) { combatManager.checkAndStartServiceForBase(base); }
    public boolean checkMissileMinRange(PlacedObject a, PlacedObject t) { return combatManager.checkMissileMinRange(a, t); }
    public PlacedObject findTarget(PlacedObject att, float range) { return combatManager.findTarget(att, range); }
    public int getTotalStored(PlacedObject unit) { return unitManager.getTotalStored(unit); }
    public PlacedObject findNearbyMineral(PlacedObject unit) { return unitManager.findNearbyMineral(unit); }
    public PlacedObject createShipFromName(String name) { return null; }  // 工厂系统已移除
    public int countMinersAt(PlacedObject mineral) { return unitManager.countMinersAt(mineral); }
    public boolean isPointInObject(float px, float py, PlacedObject o) { return com.rtnp.demo.logic.UnitUtils.isPointInObject(px, py, o); }
}