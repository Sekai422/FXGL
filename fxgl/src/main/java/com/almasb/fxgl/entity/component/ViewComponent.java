/*
 * FXGL - JavaFX Game Library. The MIT License (MIT).
 * Copyright (c) AlmasB (almaslvl@gmail.com).
 * See LICENSE for details.
 */

package com.almasb.fxgl.entity.component;

import com.almasb.fxgl.app.FXGL;
import com.almasb.fxgl.entity.Component;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.RenderLayer;
import com.almasb.fxgl.entity.view.EntityView;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.HitBox;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

/**
 * Adds a game scene view to an entity.
 * To change view of an entity use {@link #setView(Node)}.
 *
 * @author Almas Baimagambetov (AlmasB) (almaslvl@gmail.com)
 */
@CoreComponent
public class ViewComponent extends Component {

    private static Color showBBoxColor = Color.RED;

    /**
     * @param showBBoxColor the color to highlight bounding boxes
     */
    public static void setShowBBoxColor(Color showBBoxColor) {
        ViewComponent.showBBoxColor = showBBoxColor;
    }

    private boolean showBBox() {
        return FXGL.getBoolean("dev.showbbox");
    }

    /**
     * Turn on / off bounding box display.
     * Useful for debugging to see the bounds of each hit box.
     *
     * @param on on / off flag
     */
    public final void turnOnDebugBBox(boolean on) {
        if (!on) {
            removeDebugBBox();
            return;
        }

        addDebugBBox();
    }

    /**
     * The view is not reassigned since its properties are bound
     * to entity properties.
     * To alter the view - change its nodes.
     */
    private final EntityView view = new EntityView();

    private final ObjectProperty<RenderLayer> renderLayer = new SimpleObjectProperty<>(RenderLayer.DEFAULT);

    /**
     * Creates view component with no graphics.
     */
    public ViewComponent() {}

    /**
     * Creates view component with given graphics.
     *
     * @param graphics the graphics
     */
    public ViewComponent(Node graphics) {
        view.addNode(graphics);
    }

    /**
     * Creates view component with given render layer.
     *
     * @param renderLayer render layer to use for view
     */
    public ViewComponent(RenderLayer renderLayer) {
        setRenderLayer(renderLayer);
    }

    /**
     * Creates view component with given graphics and given render layer.
     *
     * @param graphics the graphics
     * @param renderLayer render layer to use for view
     */
    public ViewComponent(Node graphics, RenderLayer renderLayer) {
        view.addNode(graphics);
        setRenderLayer(renderLayer);
    }

    /**
     * @return render layer
     */
    public RenderLayer getRenderLayer() {
        return renderLayer.get();
    }

    /**
     * @return render layer property
     */
    public ObjectProperty<RenderLayer> renderLayerProperty() {
        return renderLayer;
    }

    /**
     * Set render layer for this entity.
     * Render layer determines how an entity
     * is rendered relative to other entities.
     * The layer with higher index()
     * will be rendered on top of the layer with lower index().
     * By default an
     * entity has the very top layer with highest index equal to
     * [Integer.MAX_VALUE].
     *
     * @param renderLayer the render layer
     */
    public void setRenderLayer(RenderLayer renderLayer) {
        this.renderLayer.set(renderLayer);
    }

    /**
     * @return view
     */
    public EntityView getView() {
        return view;
    }

    /**
     * Set view without generating bounding boxes from view.
     *
     * @param view the view
     */
    public void setView(Node view) {
        setView(view, false);
    }

    /**
     * Set view. The generate bbox flag tells the component
     * whether it should generate bbox from the given view.
     *
     * Note: the generated bounding box is an approximation based on
     * the layout bounds of the view object.
     *
     * @param view the view
     * @param generateBoundingBox generate bbox flag
     */
    public void setView(Node view, boolean generateBoundingBox) {
        EntityView entityView = view instanceof EntityView ? (EntityView) view : new EntityView(view);

        this.view.getNodes().setAll(entityView.getNodes());

        if (showBBox()) {
            this.view.addNode(debugBBox);
        }

        if (generateBoundingBox) {
            generateBBox();
        }
    }

    /**
     * Convenience method to set texture as view.
     *
     * @param textureName name of texture
     */
    public void setTexture(String textureName) {
        setTexture(textureName, false);
    }

    /**
     * Convenience method to set texture as view.
     *
     * @param textureName name of texture
     * @param generateBoundingBox generate bbox from view flag
     */
    public void setTexture(String textureName, boolean generateBoundingBox) {
        EntityView view = new EntityView(FXGL.getAssetLoader().loadTexture(textureName));

        setView(view, generateBoundingBox);
    }

    private PositionComponent position;
    private RotationComponent rotation;

    @Override
    public void onAdded(Entity entity) {
        bindView();

        if (showBBox()) {
            turnOnDebugBBox(true);
        }
    }

    @Override
    public void onRemoved(Entity entity) {
        view.dispose();
    }

    private void bindView() {
        getView().translateXProperty().bind(position.xProperty());
        getView().translateYProperty().bind(position.yProperty());
        getView().rotateProperty().bind(rotation.valueProperty());
    }

    private void generateBBox() {
        getEntity().getBoundingBoxComponent().clearHitBoxes();

        getEntity().getBoundingBoxComponent().addHitBox(new HitBox("__VIEW__", BoundingShape.box(
                getView().getLayoutBounds().getWidth(), getView().getLayoutBounds().getHeight()
        )));
    }

    private Group debugBBox = new Group();

    private ListChangeListener<? super HitBox> hitboxListener = c -> {
        debugBBox.getChildren().clear();
        c.getList().forEach(this::addDebugView);
    };

    private void addDebugBBox() {
        BoundingBoxComponent bbox = getEntity().getBoundingBoxComponent();

        // generate view for future boxes
        bbox.hitBoxesProperty().addListener(hitboxListener);

        // generate view for current
        bbox.hitBoxesProperty().forEach(this::addDebugView);

        getView().addNode(debugBBox);
    }

    private void addDebugView(HitBox hitBox) {
        Shape view = null;

        if (hitBox.getShape().isCircle()) {
            double radius = hitBox.getWidth() / 2;
            view = new Circle(radius, radius, radius, null);

        } else if (hitBox.getShape().isRectangle()) {
            view = new Rectangle(hitBox.getWidth(), hitBox.getHeight(), null);
        }

        if (view != null) {
            view.setStroke(showBBoxColor);

            view.setTranslateX(hitBox.getMinX());
            view.setTranslateY(hitBox.getMinY());

            debugBBox.getChildren().add(view);
        }
    }

    private void removeDebugBBox() {
        BoundingBoxComponent bbox = getEntity().getBoundingBoxComponent();

        bbox.hitBoxesProperty().removeListener(hitboxListener);

        debugBBox.getChildren().clear();
        getView().removeNode(debugBBox);
    }

    @Override
    public String toString() {
        return "View(" + getRenderLayer().name() + ")";
    }
}
