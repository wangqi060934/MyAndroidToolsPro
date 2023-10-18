package cn.wq.myandroidtoolspro.recyclerview.base;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filterable;

import cn.wq.myandroidtoolspro.R;

public class SearchRecyclerListFragment extends RecyclerListFragment
        implements SearchView.OnQueryTextListener {
    protected SearchView searchView;
    private MenuItem searchMenuItem;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(initOptionsMenuOnCreate());
    }

    /**
     * 在onCreate上调用setHasOptionsMenu(true)
     * @return
     */
    protected boolean initOptionsMenuOnCreate() {
        return true;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setEmptyText(getString(R.string.empty));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.search, menu);

        searchMenuItem = menu.findItem(R.id.search);
        if (searchMenuItem == null) {
            return;
        }
        searchView=(SearchView) MenuItemCompat.getActionView(searchMenuItem);
        if (searchView == null) {
            return;
        }
        searchView.setQueryHint(getString(R.string.hint_app_search));
        searchView.setOnQueryTextListener(this);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        RecyclerView.Adapter adapter = getRecyclerAdapter();
        if ((adapter instanceof Filterable)) {
            ((Filterable) adapter).getFilter().filter(newText);
        }
        Log.e("wangqi", "search:" + newText);
        return true;
    }

    protected void setSearchHint(String hint){
        if(searchView!=null){
            searchView.setQueryHint(hint);
        }
    }

    protected void expandSearchView() {
        if (searchMenuItem != null && !MenuItemCompat.isActionViewExpanded(searchMenuItem)) {
            MenuItemCompat.expandActionView(searchMenuItem);
        }
    }

}
