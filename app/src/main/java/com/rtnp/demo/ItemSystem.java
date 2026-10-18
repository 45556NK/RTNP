package com.rtnp.demo;

import com.rtnp.demo.core.PlacedObject;
import com.rtnp.demo.core.InventoryItem;
import com.rtnp.demo.ui.BottomBar;
import com.rtnp.demo.ui.InventoryPanel;


import android.content.Context;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.graphics.Rect;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.rtnp.demo.ui.FleetPanel;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import com.rtnp.demo.logic.MultiSelectManager;
import com.rtnp.demo.ui.UnitDetailPanel;
import com.rtnp.demo.render.RenderControl;
import com.rtnp.demo.ui.panel.RightPanel;

public class ItemSystem {
    private InventoryPanel inventoryPanel;
    private InventoryPanel buildPanel;
    private FleetPanel fleetPanel;
    private BottomBar bottomBar;
    private final UnitSystem unitSystem;
    private boolean isPlacementMode = false;
    private boolean isHandlingTouch = false;
    private boolean isHandlingBuildTouch = false;
    private boolean isFleetOpen = false;
	private Rect fleetPanelRect = new Rect();  // 记录舰队面板的矩形区域
	private boolean isMultiSelectMode = false;
	private MultiSelectManager multiSelectManager;
	private OnFleetSelectListener onFleetSelectListener;
	// 在 ItemSystem 类中添加
	private UnitDetailPanel detailPanel;
	private InventoryItem selectedItemForDetail = null;
    
    // 建筑面板拖拽检测
    private float buildTouchDownX, buildTouchDownY;
    private long buildTouchDownTime;
    private boolean buildDragDetected = false;
    private static final float DRAG_THRESHOLD = 150f;
    private static final long DRAG_TIME_THRESHOLD = 300;
	
	
	public void setMultiSelectMode(boolean mode) {
		this.isMultiSelectMode = mode;
		if (fleetPanel != null) fleetPanel.invalidate();
	}
	
	public void setMultiSelectManager(MultiSelectManager manager) {
		this.multiSelectManager = manager;
	}

	
	public boolean isMultiSelectMode() {
		return isMultiSelectMode;
	}

	
    public interface PauseListener { void onPauseButtonClicked(); }
    private PauseListener pauseListener;

    public interface OnPanelToggleListener {
        void onPanelOpened();
        void onPanelClosed();
    }
    private OnPanelToggleListener panelToggleListener;

    public interface OnFleetSelectListener {
        void onFleetUnitSelected(PlacedObject unit);
    }
    private OnFleetSelectListener fleetSelectListener;

    public ItemSystem(com.rtnp.demo.ui.InventoryPanel.AnimationHost host, final UnitSystem unitSystem, Context context) {
        this.unitSystem = unitSystem;

        // 物品栏面板
        try {
            inventoryPanel = new com.rtnp.demo.ui.InventoryPanel(host, context);
            inventoryPanel.setOnItemSelectedListener(new com.rtnp.demo.ui.InventoryPanel.OnItemSelectedListener() {
					@Override
					public void onItemSelected(InventoryItem item) {
						isPlacementMode = true;
						if (unitSystem != null) unitSystem.deselectUnit();
					}
					@Override
					public void onItemDeselected() {
						isPlacementMode = false;
					}
				});
        } catch (Throwable t) {
            inventoryPanel = null;
        }

        // 建筑面板
        try {
            buildPanel = new com.rtnp.demo.ui.InventoryPanel(host, context);
            buildPanel.setOnItemSelectedListener(new com.rtnp.demo.ui.InventoryPanel.OnItemSelectedListener() {
					@Override
					public void onItemSelected(InventoryItem item) {
						isPlacementMode = true;
						if (unitSystem != null) unitSystem.deselectUnit();
					}
					@Override
					public void onItemDeselected() {
						isPlacementMode = false;
					}
				});
        } catch (Throwable t) {
            buildPanel = null;
        }
        buildPanel.setIsBuildPanel(true);

        // 舰船管理面板
        try {
            fleetPanel = new FleetPanel(context);
			fleetPanel.setCallback(new FleetPanel.FleetPanelCallback() {
					@Override
					public List<PlacedObject> getFleetUnits() {
						// 始终返回所有己方单位（与单选模式一致）
						return unitSystem.getFleetUnits();
					}

					@Override
					public PlacedObject getSelectedUnit() {
						return unitSystem.getSelectedUnit();
					}

                    @Override
                    public void onUnitSelected(PlacedObject unit) {
                        if (unit == null) return;
                        if (isMultiSelectMode && multiSelectManager != null) {
                            Set<PlacedObject> selected = multiSelectManager.getSelectedUnits();
                            if (selected.contains(unit)) {
                                selected.remove(unit);
                                unit.isSelected = false;
                            } else {
                                selected.add(unit);
                                unit.isSelected = true;
                            }
                            if (onFleetSelectListener != null) {
                                onFleetSelectListener.onFleetUnitSelected(unit);
                            }
                        } else {
                            if (multiSelectManager != null) multiSelectManager.clearSelection();
                            unitSystem.deselectUnit();
                            // ★ 直接设置选中单位，不用坐标查找
                            unitSystem.setSelectedUnitDirect(unit);
                            unit.isSelected = true;
                            // 移动相机到单位位置
                            if (onFleetSelectListener != null) {
                                onFleetSelectListener.onFleetUnitSelected(unit);
                            }
                        }
                        if (host != null) host.requestRedraw();
                    }

					@Override
					public boolean isMultiSelectMode() {
						return isMultiSelectMode;
					}

					@Override
					public Set<PlacedObject> getSelectedUnits() {
						if (multiSelectManager != null) return multiSelectManager.getSelectedUnits();
						return Collections.emptySet();
					}
				});
			// 底部栏
			bottomBar = new BottomBar(context);
			bottomBar.setOnToggleInventoryListener(new BottomBar.OnToggleInventoryListener() {
					@Override
					public void onToggleInventory() {
						if (buildPanel != null && buildPanel.isOpen()) buildPanel.closeIfOpen();
						if (isFleetOpen) closeFleetPanel();
						if (inventoryPanel != null) inventoryPanel.toggle();
						if (bottomBar != null && inventoryPanel != null)
							bottomBar.setInventoryOpen(inventoryPanel.isOpen());
						notifyPanelToggle();
					}
				});
			bottomBar.setOnToggleBuildListener(new BottomBar.OnToggleBuildListener() {
					@Override
					public void onToggleBuild() {
						if (inventoryPanel != null && inventoryPanel.isOpen()) inventoryPanel.closeIfOpen();
						if (isFleetOpen) closeFleetPanel();
						if (buildPanel != null) buildPanel.toggle();
						if (bottomBar != null && buildPanel != null)
							bottomBar.setBuildOpen(buildPanel.isOpen());
						notifyPanelToggle();
					}
				});
		// 为物品栏设置监听器，同步选中的物品到详情栏
		detailPanel = new UnitDetailPanel();
		if (inventoryPanel != null) {
			inventoryPanel.setOnItemSelectedListener(new com.rtnp.demo.ui.InventoryPanel.OnItemSelectedListener() {
					@Override
					public void onItemSelected(InventoryItem item) {
						isPlacementMode = true;
						if (unitSystem != null) unitSystem.deselectUnit();
						// ★ 更新详情栏
						setSelectedItemForDetail(item);
					}

					@Override
					public void onItemDeselected() {
						isPlacementMode = false;
						// ★ 清除详情栏
						setSelectedItemForDetail(null);
					}
				});
		}

		// 同样为 buildPanel 设置监听器（如果也需要显示详情）
		if (buildPanel != null) {
			buildPanel.setOnItemSelectedListener(new com.rtnp.demo.ui.InventoryPanel.OnItemSelectedListener() {
					@Override
					public void onItemSelected(InventoryItem item) {
						isPlacementMode = true;
						if (unitSystem != null) unitSystem.deselectUnit();
						setSelectedItemForDetail(item);
					}

					@Override
					public void onItemDeselected() {
						isPlacementMode = false;
						setSelectedItemForDetail(null);
					}
				});
        
		}
// 注意：舰队按钮的点击已在 onTouchEvent 中通过 bottomBar.isFleetButtonHit 处理，无需重复设置监听器
        } catch (Throwable t) {
			fleetPanel = null;
		}
    }

    public void setOnFleetSelectListener(OnFleetSelectListener l) {
        this.fleetSelectListener = l;
    }
	public boolean isFleetPanelContains(float x, float y) {
		return isFleetOpen() && fleetPanelRect != null && fleetPanelRect.contains((int)x, (int)y);
	}
	public Rect getBottomBarRect() {
		// ★ 新底部栏启用时，旧底部栏不再暴露矩形（GameTouchHandler 自动跳过旧分支）
		if (RenderControl.INSTANCE.getUseNewBottomBar()) return null;
		if (bottomBar != null) return bottomBar.getRect();
		return null;
	}
	
	
    private void closeFleetPanel() {
        isFleetOpen = false;
        if (bottomBar != null) bottomBar.setFleetOpen(false);
    }

    public void setOnPanelToggleListener(OnPanelToggleListener listener) {
        this.panelToggleListener = listener;
    }

    private void notifyPanelToggle() {
        boolean anyOpen = (inventoryPanel != null && inventoryPanel.isOpen()) ||
            (buildPanel != null && buildPanel.isOpen()) || isFleetOpen;
        if (anyOpen) {
            RightPanel.INSTANCE.unlock();
            if (unitSystem != null) unitSystem.deselectUnit();
            if (panelToggleListener != null) panelToggleListener.onPanelOpened();
        } else {
            if (panelToggleListener != null) panelToggleListener.onPanelClosed();
        }
    }

    public void setPauseListener(PauseListener listener) {
        this.pauseListener = listener;
    }
	
	public void updateSelectedUnitForDetail(PlacedObject unit) {
		if (inventoryPanel != null) {
			inventoryPanel.setSelectedUnitForDetail(unit);
		}
	}

    public void updateScreenSize(int screenWidth, int screenHeight) {
		if (inventoryPanel != null) inventoryPanel.updateScreenSize(screenWidth, screenHeight);
		if (buildPanel != null) buildPanel.updateScreenSize(screenWidth, screenHeight);
		if (bottomBar != null) bottomBar.setScreenSize(screenWidth, screenHeight);
		if (fleetPanel != null) {
			int fleetWidth = screenWidth / 3;
			fleetPanel.layout(0, 0, fleetWidth, screenHeight);
			if (fleetPanelRect == null) fleetPanelRect = new Rect();
			fleetPanelRect.set(0, 0, fleetWidth, screenHeight);
		}
	}

    public boolean isPlacementMode() { return isPlacementMode; }

    public InventoryItem getSelectedItem() {
        if (inventoryPanel != null && inventoryPanel.isOpen() && inventoryPanel.getSelectedItem() != null)
            return inventoryPanel.getSelectedItem();
        if (buildPanel != null && buildPanel.isOpen() && buildPanel.getSelectedItem() != null)
            return buildPanel.getSelectedItem();
        return null;
    }

    public void closeInventoryIfOpen() {
        if (inventoryPanel != null && inventoryPanel.isOpen()) inventoryPanel.closeIfOpen();
        if (buildPanel != null && buildPanel.isOpen()) buildPanel.closeIfOpen();
    }

    public boolean isFleetOpen() { return isFleetOpen; }

    public void drawFleetPanel(Canvas canvas) {
        if (isFleetOpen && fleetPanel != null) fleetPanel.draw(canvas);
    }
	
    public boolean onTouchEvent(MotionEvent event) {
        // ★ 如果底部栏或右侧栏触摸被禁用，不处理任何事件
        if (RenderControl.INSTANCE.getTouchDisabledBottomBar() || 
            RenderControl.INSTANCE.getTouchDisabledUnitInfoPanel()) {
            return false;
        }
    
        if (isFleetOpen() && fleetPanel != null && fleetPanelRect.contains((int)event.getX(), (int)event.getY())) {
            return fleetPanel.onTouchEvent(event);
        }
        int action = event.getActionMasked();

        // 底部栏触摸
        if (action == MotionEvent.ACTION_DOWN && bottomBar != null) {
            // ★ 新底部栏启用时，旧底部栏不再响应触摸
            if (RenderControl.INSTANCE.getUseNewBottomBar()) return false;
            if (RenderControl.INSTANCE.getTouchDisabledBottomBar()) return false;
            if (bottomBar.contains(event.getX(), event.getY())) {
                // 暂停按钮
                if (bottomBar.isPauseButtonHit(event.getX(), event.getY())) {
                    if (pauseListener != null) pauseListener.onPauseButtonClicked();
                    return true;
                }
                // 建筑按钮 - 打开时自动关闭物品栏
                if (bottomBar.isBuildButtonHit(event.getX(), event.getY())) {
                    if (buildPanel != null) {
                        buildPanel.toggle();
                        if (buildPanel.isOpen() && inventoryPanel != null && inventoryPanel.isOpen()) {
                            inventoryPanel.closeIfOpen();
                        }
                        if (bottomBar != null) {
                            bottomBar.setBuildOpen(buildPanel.isOpen());
                            bottomBar.setInventoryOpen(inventoryPanel != null && inventoryPanel.isOpen());
                        }
                        notifyPanelToggle();
                    }
                    return true;
                }
                // 舰队按钮 - 独立切换
                if (bottomBar.isFleetButtonHit(event.getX(), event.getY())) {
                    isFleetOpen = !isFleetOpen;
                    if (bottomBar != null) bottomBar.setFleetOpen(isFleetOpen);
                    if (isFleetOpen && fleetPanel != null) {
                        fleetPanel.refreshFilteredUnits();
                    }
                    notifyPanelToggle();
                    return true;
                }
                // 物品按钮 - 打开时自动关闭建筑面板
                if (bottomBar.isInventoryButtonHit(event.getX(), event.getY())) {
                    if (inventoryPanel != null) {
                        inventoryPanel.toggle();
                        if (inventoryPanel.isOpen() && buildPanel != null && buildPanel.isOpen()) {
                            buildPanel.closeIfOpen();
                        }
                        if (bottomBar != null) {
                            bottomBar.setInventoryOpen(inventoryPanel.isOpen());
                            bottomBar.setBuildOpen(buildPanel != null && buildPanel.isOpen());
                        }
                        notifyPanelToggle();
                    }
                    return true;
                }
            }
        }
        // 舰队面板触摸 (左侧区域)
        if (isFleetOpen && fleetPanel != null) {
            float x = event.getX();
            if (x >= 0 && x <= fleetPanel.getWidth()) {
                if (fleetPanel.onTouchEvent(event)) return true;
                return true; // 消费事件，防止穿透到地图
            }
        }

        // 建筑面板触摸 - 支持拖拽切换模式
        if (action == MotionEvent.ACTION_DOWN && buildPanel != null && buildPanel.isOpen() && !buildPanel.isAnimating()) {
            if (buildPanel.contains(event.getX(), event.getY())) {
                isHandlingBuildTouch = true;
                buildTouchDownX = event.getX();
                buildTouchDownY = event.getY();
                buildTouchDownTime = System.currentTimeMillis();
                buildDragDetected = false;
                buildPanel.onTouchEvent(event);
                return true;
            }
        }
        if (isHandlingBuildTouch && buildPanel != null && buildPanel.isOpen() && !buildPanel.isAnimating()) {
            if (action == MotionEvent.ACTION_MOVE && !buildDragDetected) {
                float dx = event.getX() - buildTouchDownX;
                float dy = event.getY() - buildTouchDownY;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);
                long elapsed = System.currentTimeMillis() - buildTouchDownTime;
                
                if (distance > DRAG_THRESHOLD && elapsed < DRAG_TIME_THRESHOLD) {
                    // 快速拖动 → 进入自由移动模式
                    buildDragDetected = true;
                    // 通过回调通知外部重绘
                    if (panelToggleListener != null) panelToggleListener.onPanelOpened();
                    return true; // 消费事件，切换到地图拖动模式
                }
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                if (!buildDragDetected) {
                    buildPanel.onTouchEvent(event);
                }
                isHandlingBuildTouch = false;
                buildDragDetected = false;
            } else if (!buildDragDetected) {
                buildPanel.onTouchEvent(event);
            }
            return true;
        }

        // 物品栏触摸
        if (isHandlingTouch && inventoryPanel != null && inventoryPanel.isOpen() && !inventoryPanel.isAnimating()) {
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                inventoryPanel.onTouchEvent(event);
                isHandlingTouch = false;
            } else {
                inventoryPanel.onTouchEvent(event);
            }
            return true;
        }
        if (action == MotionEvent.ACTION_DOWN && inventoryPanel != null && inventoryPanel.isOpen() && !inventoryPanel.isAnimating()) {
            if (inventoryPanel.contains(event.getX(), event.getY())) {
                isHandlingTouch = true;
                inventoryPanel.onTouchEvent(event);
                return true;
            }
        }

        return false;
    }
	public void refreshFleetPanel() {
		if (fleetPanel != null) {
			fleetPanel.refreshFilteredUnits();
		}
	}
	public void setSelectedItemForDetail(InventoryItem item) {
		this.selectedItemForDetail = item;
	}
    // 在 ItemSystem 类中添加这三个方法

    /**
     * 获取物品栏是否打开
     */
    public boolean isInventoryOpen() {
        return inventoryPanel != null && inventoryPanel.isOpen();
    }

    /**
     * 获取建筑面板是否打开
     */
    public boolean isBuildOpen() {
        return buildPanel != null && buildPanel.isOpen();
    }
    /**
 * 建筑面板是否触发了拖拽模式（用于地图层处理）
 */
    public boolean isBuildDragMode() {
        return buildDragDetected;
    }
    /**
 * 重置建筑面板拖拽模式
 */
    public void resetBuildDragMode() {
        buildDragDetected = false;
    }
    public void draw(Canvas canvas) {
		// 绘制舰队面板（左侧）
		if (isFleetOpen && fleetPanel != null) fleetPanel.draw(canvas);

		// 绘制物品栏和建筑面板（右侧）
		if (inventoryPanel != null) inventoryPanel.draw(canvas);
		if (buildPanel != null) buildPanel.draw(canvas);

		// ★ 详情栏独立绘制在物品栏/建筑面板上方
		// 检测物品栏或建筑面板是否有选中的物品且处于打开状态
		boolean inventoryHasSelection = (inventoryPanel != null && inventoryPanel.isOpen() && 
			inventoryPanel.getSelectedItem() != null);
		boolean buildHasSelection = (buildPanel != null && buildPanel.isOpen() && 
			buildPanel.getSelectedItem() != null);

		// 确定要使用的面板和选中的物品
		InventoryPanel targetPanel = null;
		InventoryItem targetItem = null;

		if (inventoryHasSelection) {
			targetPanel = inventoryPanel;
			targetItem = inventoryPanel.getSelectedItem();
		} else if (buildHasSelection) {
			targetPanel = buildPanel;
			targetItem = buildPanel.getSelectedItem();
		}

		if (targetItem != null && targetPanel != null && detailPanel != null) {
			int screenWidth = canvas.getWidth();
			int screenHeight = canvas.getHeight();
			int panelWidth = screenWidth / 2;
			int panelLeft = screenWidth - panelWidth;

			// 获取目标面板的动画进度
			float animProgress = targetPanel.getAnimProgress();
			float panelTop = screenHeight - (screenHeight / 2) * animProgress;

			int detailHeight = detailPanel.getPanelHeight();
			int margin = 8;
			int detailTop = (int) panelTop - detailHeight - margin;
			if (detailTop < 0) detailTop = 0;

			detailPanel.draw(canvas, panelLeft + margin, detailTop, 
							 panelWidth - margin * 2, targetItem);
		}

        // 底部栏（盖在最下层）
        // ★ 新底部栏启用时，旧底部栏不再渲染
        if (bottomBar != null && !RenderControl.INSTANCE.getUseNewBottomBar()) {
            bottomBar.setInventoryOpen(inventoryPanel != null && inventoryPanel.isOpen());
            bottomBar.setBuildOpen(buildPanel != null && buildPanel.isOpen());
            bottomBar.setFleetOpen(isFleetOpen);
            bottomBar.draw(canvas);
        }
	}
}