package cn.wq.navigationview;

import android.content.Context;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.view.menu.MenuItemImpl;
import android.support.v7.view.menu.SubMenuBuilder;
import android.view.SubMenu;

/**
 * This is a {@link MenuBuilder} that returns an instance of {@link NavigationSubMenu} instead of
 * {@link SubMenuBuilder} when a sub menu is created.
 */
public class NavigationMenu extends MenuBuilder {

    public NavigationMenu(Context context) {
        super(context);
    }

    @Override
    public SubMenu addSubMenu(int group, int id, int categoryOrder, CharSequence title) {
        final MenuItemImpl item = (MenuItemImpl) addInternal(group, id, categoryOrder, title);
        final SubMenuBuilder subMenu = new NavigationSubMenu(getContext(), this, item);
        item.setSubMenu(subMenu);
        return subMenu;
    }

}
