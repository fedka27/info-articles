package test.infoapp.ui.list;

import io.reactivex.disposables.CompositeDisposable;
import test.infoapp.injection.model.data.dto.Link;
import test.infoapp.injection.model.data.dto.Spoiler;
import test.infoapp.injection.model.interactors.ViewInteractor;
import test.infoapp.injection.model.managers.AdsManager;
import test.infoapp.injection.model.repositories.ConfigRepository;
import test.infoapp.injection.model.repositories.ContentRepository;
import test.infoapp.util.L;
import test.infoapp.util.rx.RxSchedulersAbs;

public class ListPresenter implements ListContract.Presenter {
    private static final String TAG = ListPresenter.class.getSimpleName();
    private static final String KEY_LIST = TAG + "_LIST";
    private ListContract.View view;

    private ConfigRepository configRepository;
    private ContentRepository contentRepository;
    private AdsManager adsManager;
    private RxSchedulersAbs rxSchedulersAbs;
    private ViewInteractor viewInteractor;
    private CompositeDisposable compositeDisposable;

    private boolean isShowAdsByFirstClick;

    public ListPresenter(ConfigRepository configRepository,
                         ContentRepository contentRepository,
                         AdsManager adsClickManager,
                         RxSchedulersAbs rxSchedulersAbs,
                         ViewInteractor viewInteractor,
                         CompositeDisposable compositeDisposable) {
        this.configRepository = configRepository;
        this.contentRepository = contentRepository;
        this.adsManager = adsClickManager;
        this.rxSchedulersAbs = rxSchedulersAbs;
        this.viewInteractor = viewInteractor;
        this.compositeDisposable = compositeDisposable;

        isShowAdsByFirstClick = true;
    }

    @Override
    public void setView(ListContract.View view) {
        this.view = view;
    }

    @Override
    public void onStart() {
        this.view.configureAds(configRepository.getConfigSaved());
        loadData();
    }

    private void loadData() {
        compositeDisposable.clear();
        compositeDisposable.add(contentRepository.content()
                .compose(rxSchedulersAbs.getIOToMainTransformer())
                .compose(viewInteractor.manageProgressObservable(view))
                .doOnNext(content -> {
                    view.loadBgOrParseColor(content.getImageBg(), content.getBgColor());
                    view.setList(content.getItems());
                })
                .subscribe(v -> {
                }, throwable -> viewInteractor.manageError(view, throwable)));

    }

    @Override
    public void onClick(Spoiler spoiler) {
        boolean isShowAds = adsManager.clickToLinkAndIsShowAds();
        L.d(TAG, "isShowAds - " + isShowAds);
        if (isShowAdsByFirstClick) {
            isShowAdsByFirstClick = false;
            adsManager.showAdsOfType(configRepository.getConfigSaved(), view);
        }
        if (isShowAds) {
            adsManager.showAdsOfType(configRepository.getConfigSaved(), view);
        }
    }

    @Override
    public void onLinkPressed(Link link) {
        view.showProgressWebDialog(link.getText(), link.getLink());
    }

    @Override
    public void onRefresh() {
        loadData();
    }

    @Override
    public void onStop() {
        compositeDisposable.clear();
    }
}
