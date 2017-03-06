/**
 * Copyright (c) 2014-2017 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.ui.items;

import org.eclipse.emf.common.util.EList;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.model.sitemap.LinkableWidget;
import org.eclipse.smarthome.model.sitemap.Sitemap;
import org.eclipse.smarthome.model.sitemap.Widget;

/**
 * This interface is used by a service which combines the core item registry
 * with an aggregation of item ui providers; it can be therefore widely used for
 * all UI related information requests regarding items.
 *
 * @author Kai Kreuzer - Initial contribution and API
 * @author Chris Jackson
 *
 */
public interface ItemUIRegistry extends ItemRegistry, ItemUIProvider {

    /**
     * Retrieves the label for a widget.
     *
     * This first checks, if there is a label defined in the sitemap. If not, it
     * checks all item UI providers for a label. If no label can be found, it is
     * set to an empty string.
     *
     * If the label contains a "[%format]" section, i.e.
     * "[%s]" for a string or "[%.3f]" for a decimal, this is replaced by the
     * current value of the item and padded by a "<span>" element.
     *
     * @param w
     *            the widget to retrieve the label for
     * @return the label to use for the widget
     */
    public String getLabel(Widget w);

    /**
     * Retrieves the category for a widget.
     *
     * This first checks, if there is a category defined in the sitemap. If not, it
     * checks all item UI providers for a category. If no category can be found, the
     * default category is the widget type name, e.g. "switch".
     *
     * @param w
     *            the widget to retrieve the category for
     * @return the category to use for the widget
     */
    public String getCategory(Widget w);

    /**
     * Retrieves the current state of the item of a widget or <code>UnDefType.UNDEF</code>.
     *
     * @param w
     *            the widget to retrieve the item state for
     * @return the item state of the widget
     */
    public State getState(Widget w);

    /**
     * Retrieves the widget for a given id on a given sitemap.
     *
     * @param sitemap
     *            the sitemap to look for the widget
     * @param id
     *            the id of the widget to look for
     * @return the widget for the given id
     */
    public Widget getWidget(Sitemap sitemap, String id);

    /**
     * Provides an id for a widget.
     *
     * This constructs a string out of the position of the sitemap, so if this
     * widget is the third child of a page linked from the fifth widget on the
     * home screen, its id would be "0503". If the widget is dynamically created
     * and not available in the sitemap, the name of its associated item is used
     * instead.
     *
     * @param w
     *            the widget to get the id for
     * @return an id for this widget
     */
    public String getWidgetId(Widget w);

    /**
     * this should be used instead of LinkableWidget.getChildren() as there
     * might be no children defined on the widget, but they should be
     * dynamically determined by looking at the members of the underlying item.
     *
     * @param w
     *            the widget to retrieve the children for
     * @return the (dynamically or statically defined) children of the widget
     */
    public EList<Widget> getChildren(LinkableWidget w);

    /**
     * Gets the label color for the widget. Checks conditional statements to
     * find the color based on the item value
     *
     * @param w
     *            Widget
     * @return String with the color
     */
    public String getLabelColor(Widget w);

    /**
     * Gets the value color for the widget. Checks conditional statements to
     * find the color based on the item value
     *
     * @param w
     *            Widget
     * @return String with the color
     */
    public String getValueColor(Widget w);

    /**
     * Gets the widget visibility based on the item state
     *
     * @param w
     *            Widget
     * @return true if the item is visible
     */
    public boolean getVisiblity(Widget w);

    /**
     * Gets the item state
     *
     * @param itemName
     *            item name
     * @return State of the item
     */
    public State getItemState(String itemName);
}
