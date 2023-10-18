package cn.wq.navigationview;

import android.content.Context;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.view.menu.MenuItemImpl;
import android.support.v7.view.menu.SubMenuBuilder;

/**
 * This is a {@link SubMenuBuilder} that it notifies the parent {@link NavigationMenu} of its menu
 * updates.
 */
public class NavigationSubMenu extends SubMenuBuilder {

    public NavigationSubMenu(Context context, NavigationMenu menu, MenuItemImpl item) {
        super(context, menu, item);
    }

    @Override
    public void onItemsChanged(boolean structureChanged) {
        super.onItemsChanged(structureChanged);
        ((MenuBuilder) getParentMenu()).onItemsChanged(structureChanged);
    }

}
