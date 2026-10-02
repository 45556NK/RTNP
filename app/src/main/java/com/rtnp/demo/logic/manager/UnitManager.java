package com.rtnp.demo.logic.manager;

import android.content.Context;
import android.graphics.Color;
import com.rtnp.demo.UnitSystem;
import com.rtnp.demo.core.PlacedObject;
import com.rtnp.demo.core.InventoryItem;
import com.rtnp.demo.core.MissileType;
import com.rtnp.demo.core.MissileTypeRegistry;
import com.rtnp.demo.movement.ModuleManager;
import com.rtnp.demo.movement.ItemManager;
import com.rtnp.demo.ui.InventoryPanel;
import com.rtnp.demo.logic.UnitEffectManager;
import com.rtnp.demo.logic.HexGridManager;
import com.rtnp.demo.image.ImageManager;
import com.rtnp.demo.physics.PhysicsWorld;
import com.rtnp.demo.ai.AiManager;
import com.rtnp.demo.factory.FactorySlotConfig;

import java.util.*;

/**
 * 单位管理器 - 管理单位的创建、注册、查询
 */
public class UnitManager {

    private final List<PlacedObject> placedObjects;
    private final Map<PlacedObject, Float> healthMap;
    private final Map<String, InventoryItem> itemTemplates = new HashMap<>();
    private final Context context;
    private final UnitSystem unitSystem;

    public UnitManager(List<PlacedObject> placedObjects, Map<PlacedObject, Float> healthMap, Context context, UnitSystem unitSystem) {
        this.placedObjects = placedObjects;
        this.healthMap = healthMap;
        this.context = context;
        this.unitSystem = unitSystem;
    }

    public PlacedObject addObjectFromItem(InventoryItem item, float worldX, float worldY) {
        PlacedObject obj = new PlacedObject();
        obj.worldX = worldX;
        obj.worldY = worldY;
        obj.shape = item.shape;
        obj.size = item.size;
        obj.width = item.width;
        obj.height = item.height;
        obj.health = item.health;
        obj.originalHealth = item.health;
        obj.name = item.name;
        obj.type = item.type;
        obj.speed = item.speed;
        obj.acceleration = item.acceleration;
        obj.maxSpeed = item.maxSpeed;
        obj.currentSpeed = 0f;
        obj.deceleration = item.deceleration > 0 ? item.deceleration : 20f;
        obj.turnRate = item.turnRate;
        obj.canMove = !("environment".equals(item.type) || "base".equals(item.type) || item.autoControl);
        obj.faction = item.faction;
        obj.category = item.category;
        obj.color = item.color;
        obj.autoControl = item.autoControl;
        obj.description = item.description;
        obj.missileTypeName = item.missileTypeName;
        obj.storedItemName = item.storedItemName;
        obj.storedItemCount = item.storedItemCount;
        obj.miningSpeed = item.miningSpeed;
        obj.storageCapacity = item.storageCapacity;
        obj.miningEnabled = item.miningEnabled;
        obj.maxMiners = item.maxMiners;
        obj.costItem = item.costItem;
        obj.costAmount = item.costAmount;
        obj.effectId = item.effectId;
        obj.effectOffsetX = item.effectOffsetX;
        obj.effectOffsetY = item.effectOffsetY;
        obj.effectAngleOffset = item.effectAngleOffset;
        obj.effectTriggerSpeed = item.effectTriggerSpeed;
        obj.maxDockingSlots = item.maxDockingSlots;
        obj.dockRadius = item.dockRadius;
        obj.traitIds = new ArrayList<>(item.traitIds);
        obj.strategySlots = item.strategySlots;
        obj.strategyIds = new ArrayList<>(item.strategyIds);
        obj.allowWeaponChange = item.allowWeaponChange;
        obj.allowStrategyChange = item.allowStrategyChange;
        obj.textureDisplayHeight = item.textureDisplayHeight;
        obj.aiIds = new ArrayList<>(item.aiIds);
        obj.playerControllable = item.playerControllable;

        // ★ 工厂槽初始化（固定槽位数量）
        obj.factorySlots = item.factorySlots;
        obj.factorySlotConfigs = new ArrayList<>();
        for (int i = 0; i < obj.factorySlots; i++) {
            if (i < item.factorySlotConfigs.size()) {
                obj.factorySlotConfigs.add(item.factorySlotConfigs.get(i).copy());
            } else {
                obj.factorySlotConfigs.add(new FactorySlotConfig());  // 空槽
            }
        }
        obj.factoryCooldownBars = new ArrayList<>();
        
        // ★ 立场槽初始化
        obj.shieldSlots = item.shieldSlots;
        obj.shieldSlotConfigs = new ArrayList<>();
        for (int i = 0; i < obj.shieldSlots; i++) {
            if (i < item.shieldSlotConfigs.size() && item.shieldSlotConfigs.get(i) != null) {
                obj.shieldSlotConfigs.add(item.shieldSlotConfigs.get(i).copy());
            } else {
                obj.shieldSlotConfigs.add(new com.rtnp.demo.shield.ShieldSlotConfig());
            }
        }
        obj.allowShieldChange = item.allowShieldChange;
        for (int i = 0; i < obj.factorySlots; i++) {
            obj.factoryCooldownBars.add(null);
        }
        obj.allowFactoryChange = item.allowFactoryChange;

        HexGridManager.INSTANCE.registerUnit(obj);

        obj.physicsMass = item.physicsMass;
        obj.uniqueId = PlacedObject.generateUniqueId();

        obj.maxLifetime = item.lifetime;
        obj.lifetime = 0f;
        obj.strategyCooldownBars = new ArrayList<>();
        obj.suppressionEffectId = item.suppressionEffectId;
        obj.buildSpeed = item.buildSpeed;
        obj.buildDockSlots = item.buildDockSlots;
        obj.buildDockRadius = item.buildDockRadius;
        for (int i = 0; i < obj.strategySlots; i++) {
            obj.strategyCooldownBars.add(null);
        }

        UnitEffectManager.INSTANCE.registerFromTemplate(obj, item);

        if (item.useTexture && item.texturePath != null && !item.texturePath.isEmpty() && context != null) {
            try {
                android.graphics.Bitmap bmp = ImageManager.getInstance(context).getBitmap(item.texturePath);
                if (bmp != null) {
                    obj.textureBitmap = bmp;
                }
            } catch (Exception e) {
                obj.textureBitmap = null;
            }
        }

        if (!item.missileTypeName.isEmpty()) {
            MissileType mt = MissileTypeRegistry.get(item.missileTypeName);
            if (mt != null) {
                obj.missileCooldownMax = mt.cooldownMax;
                obj.missileMinRange = mt.minRange;
            }
        }
        if (item.refVolumeShape != null && !item.refVolumeShape.isEmpty() && item.refVolumeRadius > 0) {
            PlacedObject.ReferenceVolume rv = new PlacedObject.ReferenceVolume();
            rv.shape = item.refVolumeShape;
            rv.radius = item.refVolumeRadius;
            obj.referenceVolume = rv;
        }

        // 武器槽位
        obj.weaponSlots.clear();
        if (item.weaponSlotCount >= 0) {
            while (obj.weaponSlots.size() < item.weaponSlotCount) {
                PlacedObject.WeaponSlot emptySlot = new PlacedObject.WeaponSlot();
                emptySlot.type = "?";
                emptySlot.active = false;
                emptySlot.templateId = null;
                obj.weaponSlots.add(emptySlot);
            }
            while (obj.weaponSlots.size() > item.weaponSlotCount) {
                obj.weaponSlots.remove(obj.weaponSlots.size() - 1);
            }
        } else if (item.weaponSlots.isEmpty()) {
            PlacedObject.WeaponSlot defaultSlot = new PlacedObject.WeaponSlot();
            defaultSlot.type = item.weaponType;
            defaultSlot.range = item.range;
            defaultSlot.damage = item.damage;
            defaultSlot.bulletSpeed = item.bulletSpeed;
            defaultSlot.chargeTime = item.chargeTime;
            defaultSlot.minDamage = item.minDamage;
            defaultSlot.maxDamage = item.maxDamage;
            defaultSlot.active = item.weaponActive;
            obj.weaponSlots.add(defaultSlot);
        } else {
            for (PlacedObject.WeaponSlot ws : item.weaponSlots) {
                obj.weaponSlots.add(copySlot(ws));
            }
        }
        obj.syncCurrentWeapon();
        unitSystem.updateMissileAttributesFromSlots(obj);

        // 不再创建 factory 和 port
        obj.displayName = generateUniqueDisplayName(obj.name);

        if ("environment".equals(obj.type)) {
            if (obj.dockedUnits == null) {
                obj.dockedUnits = new ArrayList<>();
            }
        }

        if (item.storedItemName != null && !item.storedItemName.isEmpty() && item.storedItemCount > 0) {
            if (obj.collectedItems == null) {
                obj.collectedItems = new HashMap<>();
            }
            obj.collectedItems.put(item.storedItemName, item.storedItemCount);
        }

        placedObjects.add(obj);
        healthMap.put(obj, (float) obj.health);

        if (unitSystem.getPhysicsWorld() != null) {
            unitSystem.getPhysicsWorld().addUnit(obj);
        }

        if (obj.maxLifetime > 0) {
            unitSystem.unitsWithLifetime.add(obj);
        }
        for (String aiId : obj.aiIds) {
            AiManager.INSTANCE.addAi(obj, aiId);
        }
        return obj;
    }

    public PlacedObject createShipFromName(String name) {
        InventoryItem template = InventoryPanel.getTemplateByName(name);
        if (template == null) {
            template = new InventoryItem();
            template.name = name;
            template.type = "unit";
            template.shape = "rectangle";
            template.width = 90;
            template.height = 30;
            template.health = 1000;
            template.speed = 80;
            template.maxSpeed = 160f;
            template.acceleration = 20f;
            template.deceleration = 40f;
            template.turnRate = 180f;
            template.faction = 2;
            template.color = Color.WHITE;
            template.costItem = null;
            template.costAmount = 0;
        }

        PlacedObject s = new PlacedObject();
        UnitEffectManager.INSTANCE.registerFromTemplate(s, template);

        s.type = template.type;
        s.name = template.name;
        s.shape = template.shape;
        s.size = template.size;
        s.width = template.width;
        s.height = template.height;
        s.health = template.health;
        s.originalHealth = template.health;
        s.speed = template.speed;
        s.acceleration = template.acceleration;
        s.maxSpeed = template.maxSpeed;
        s.deceleration = template.deceleration;
        s.currentSpeed = 0f;
        s.turnRate = template.turnRate;
        s.faction = template.faction;
        s.category = template.category;
        s.color = template.color;
        s.canMove = !("environment".equals(template.type) || "base".equals(template.type) || template.autoControl);
        s.autoControl = template.autoControl;
        s.description = template.description;
        s.missileTypeName = template.missileTypeName;
        s.storedItemName = template.storedItemName;
        s.storedItemCount = template.storedItemCount;
        s.miningSpeed = template.miningSpeed;
        s.storageCapacity = template.storageCapacity;
        s.miningEnabled = template.miningEnabled;
        s.maxMiners = template.maxMiners;
        s.costItem = template.costItem;
        s.costAmount = template.costAmount;
        
        s.maxLifetime = template.lifetime;
        s.lifetime = 0f;

        s.effectId = template.effectId;
        s.effectOffsetX = template.effectOffsetX;
        s.effectOffsetY = template.effectOffsetY;
        s.effectAngleOffset = template.effectAngleOffset;
        s.effectTriggerSpeed = template.effectTriggerSpeed;
        s.maxDockingSlots = template.maxDockingSlots;
        s.dockRadius = template.dockRadius;

        s.traitIds = new ArrayList<>(template.traitIds);
        s.strategySlots = template.strategySlots;
        s.strategyIds = new ArrayList<>(template.strategyIds);
        s.allowWeaponChange = template.allowWeaponChange;
        s.allowStrategyChange = template.allowStrategyChange;
        s.textureDisplayHeight = template.textureDisplayHeight;
        s.aiIds = new ArrayList<>(template.aiIds);

        // ★ 工厂槽初始化（固定槽位数量）
        s.factorySlots = template.factorySlots;
        s.factorySlotConfigs = new ArrayList<>();
        for (int i = 0; i < s.factorySlots; i++) {
            if (i < template.factorySlotConfigs.size()) {
                s.factorySlotConfigs.add(template.factorySlotConfigs.get(i).copy());
            } else {
                s.factorySlotConfigs.add(new FactorySlotConfig());  // 空槽
            }
        }
        s.factoryCooldownBars = new ArrayList<>();
        for (int i = 0; i < s.factorySlots; i++) {
            s.factoryCooldownBars.add(null);
        }
        s.allowFactoryChange = template.allowFactoryChange;

        s.physicsMass = template.physicsMass;

        s.strategyCooldownBars = new ArrayList<>();
        s.suppressionEffectId = template.suppressionEffectId;
        
        s.buildSpeed = template.buildSpeed;
        s.buildDockSlots = template.buildDockSlots;
        s.buildDockRadius = template.buildDockRadius;
        for (int i = 0; i < s.strategySlots; i++) {
            s.strategyCooldownBars.add(null);
        }

        if (template.useTexture && template.texturePath != null && !template.texturePath.isEmpty() && context != null) {
            try {
                android.graphics.Bitmap bmp = ImageManager.getInstance(context).getBitmap(template.texturePath);
                s.textureBitmap = bmp;
            } catch (Exception e) { s.textureBitmap = null; }
        }

        s.weaponSlots.clear();
        if (template.weaponSlotCount >= 0) {
            while (s.weaponSlots.size() < template.weaponSlotCount) {
                PlacedObject.WeaponSlot emptySlot = new PlacedObject.WeaponSlot();
                emptySlot.type = "?";
                emptySlot.active = false;
                emptySlot.templateId = null;
                s.weaponSlots.add(emptySlot);
            }
            while (s.weaponSlots.size() > template.weaponSlotCount) {
                s.weaponSlots.remove(s.weaponSlots.size() - 1);
            }
        } else if (template.weaponSlots.isEmpty()) {
            PlacedObject.WeaponSlot defaultSlot = new PlacedObject.WeaponSlot();
            defaultSlot.type = template.weaponType;
            defaultSlot.range = template.range;
            defaultSlot.damage = template.damage;
            defaultSlot.bulletSpeed = template.bulletSpeed;
            defaultSlot.chargeTime = template.chargeTime;
            defaultSlot.minDamage = template.minDamage;
            defaultSlot.maxDamage = template.maxDamage;
            defaultSlot.active = template.weaponActive;
            s.weaponSlots.add(defaultSlot);
        } else {
            for (PlacedObject.WeaponSlot ws : template.weaponSlots) {
                s.weaponSlots.add(ws.copy());
            }
        }
        s.syncCurrentWeapon();
        unitSystem.updateMissileAttributesFromSlots(s);

        // 不再创建 factory 和 port
        s.displayName = generateUniqueDisplayName(s.name);
        HexGridManager.INSTANCE.registerUnit(s);

        if (template.storedItemName != null && !template.storedItemName.isEmpty() && template.storedItemCount > 0) {
            if (s.collectedItems == null) s.collectedItems = new HashMap<>();
            s.collectedItems.put(template.storedItemName, template.storedItemCount);
        }

        if (unitSystem.getPhysicsWorld() != null) {
            unitSystem.getPhysicsWorld().addUnit(s);
        }
        for (String aiId : s.aiIds) {
            AiManager.INSTANCE.addAi(s, aiId);
        }

        return s;
    }

    private PlacedObject.WeaponSlot copySlot(PlacedObject.WeaponSlot original) {
        return original.copy();
    }

    private String generateUniqueDisplayName(String baseName) {
        int count = 1;
        for (PlacedObject obj : placedObjects) {
            if (obj.displayName != null && obj.displayName.startsWith(baseName + "_")) {
                try {
                    int num = Integer.parseInt(obj.displayName.substring(baseName.length() + 1));
                    if (num >= count) count = num + 1;
                } catch (NumberFormatException e) { }
            } else if (obj.name != null && obj.name.equals(baseName)) {
                count++;
            }
        }
        return baseName + "_" + count;
    }

    public List<PlacedObject> getFleetUnits() {
        List<PlacedObject> fleet = new ArrayList<>();
        for (PlacedObject obj : placedObjects) {
            if (obj.faction == 2 && ("unit".equals(obj.type) || "base".equals(obj.type))) {
                fleet.add(obj);
            }
        }
        return fleet;
    }

    public List<PlacedObject> getAllUnits() {
        return placedObjects;
    }

    public int getObjectCount() {
        return placedObjects.size();
    }

    public void registerItemTemplate(InventoryItem item) {
        if (item != null && item.name != null) {
            itemTemplates.put(item.name, item);
        }
    }

    public int getTotalStored(PlacedObject unit) {
        if (unit.collectedItems == null) return 0;
        int total = 0;
        for (Map.Entry<String, Integer> entry : unit.collectedItems.entrySet()) {
            int count = entry.getValue();
            ItemManager.ItemDef def = ItemManager.getInstance().getItemDef(entry.getKey());
            int space = (def != null) ? def.space : 1;
            total += count * space;
        }
        return total;
    }

    public int getServiceCount(PlacedObject base) {
        int c = 0;
        for (PlacedObject u : placedObjects) {
            if (u.dockedAt == base) c++;
        }
        return c;
    }

    public int countMinersAt(PlacedObject mineral) {
        int count = 0;
        for (PlacedObject u : placedObjects) {
            if (u.isMining && u.miningTarget == mineral) count++;
        }
        return count;
    }

    public PlacedObject findNearbyMineral(PlacedObject unit) {
        if (unit.miningSpeed <= 0) return null;
        for (PlacedObject obj : placedObjects) {
            if (obj == unit) continue;
            if (!"environment".equals(obj.type)) continue;
            if (obj.storedItemName == null || obj.storedItemName.isEmpty() || obj.storedItemCount <= 0) continue;
            if (obj.maxMiners > 0 && countMinersAt(obj) >= obj.maxMiners) continue;
            float dx = unit.worldX - obj.worldX;
            float dy = unit.worldY - obj.worldY;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            float effectiveRange = (obj.size / 2f) + Math.max(unit.width, unit.height) / 2f + 20f;
            if (dist < effectiveRange) {
                return obj;
            }
        }
        return null;
    }

    public PlacedObject findNearbyMineral(float wx, float wy, PlacedObject mover) {
        for (PlacedObject obj : placedObjects) {
            if (obj == mover) continue;
            if (!"environment".equals(obj.type) || obj.maxMiners <= 0) continue;
            if (obj.storedItemName == null || obj.storedItemName.isEmpty() || obj.storedItemCount <= 0) continue;
            float dx = wx - obj.worldX;
            float dy = wy - obj.worldY;
            float range = (obj.size / 2f) + Math.max(mover.width, mover.height) / 2f + 20f;
            if (dx * dx + dy * dy <= range * range) return obj;
        }
        return null;
    }
}