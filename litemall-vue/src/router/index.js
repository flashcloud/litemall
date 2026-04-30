import Vue from 'vue';
import Router from 'vue-router';
import { getLocalStorage } from '@/utils/local-storage';

// import home from './home';
import shop from './shop';
import items from './items';
import user from './user';
import order from './order';
import login from './login';
import store from '../store/index';

Vue.use(Router);

const RouterModel = new Router({
  routes: [...shop, ...items, ...user, ...order, ...login]
});

// 辅助函数：继续导航流程
function continueNavigation(to, next) {
  console.log(to.meta, "meta");
  // 页面顶部菜单拦截
  let emptyObj = JSON.stringify(to.meta) == "{}";
  let undefinedObj = typeof(to.meta.showHeader) == "undefined";
  if (!emptyObj && !undefinedObj) {
    store.commit("CHANGE_HEADER", to.meta);
  } else {
    store.commit("CHANGE_HEADER", { showHeader: true, title: "" });
  }
  next();
}

RouterModel.beforeEach((to, from, next) => {
  const { Authorization } = getLocalStorage('Authorization');
  
  if (!Authorization) {
    if (to.meta.login) {
      console.log("login");
      next({ name: 'login', query: { redirect: to.name } });
      return;
    }
    // 未登录用户，继续正常流程
    continueNavigation(to, next);
    return;
  }
  
  // 已登录用户，检查是否需要绑定手机号
  // 排除绑定手机页面本身和登录相关页面，避免循环跳转
  const skipCheckRoutes = ['bindPhone', 'login', 'registerGetCode', 'registerSubmit', 'registerStatus', 'forget', 'forgetReset', 'forgetStatus'];
  if (!skipCheckRoutes.includes(to.name)) {
    // 异步获取用户信息检查手机号
    import('@/api/api').then(({ authInfo }) => {
      authInfo().then(res => {
        const userMobile = res.data && res.data.data ? res.data.data.mobile : null;
        if (!userMobile || userMobile === '') {
          // 手机号为空，强制跳转到绑定手机页面
          console.log("用户未绑定手机号，强制跳转");
          next({ name: 'bindPhone' });
          return;
        }
        // 手机号已绑定，继续正常流程
        continueNavigation(to, next);
      }).catch(() => {
        // 如果获取用户信息失败，继续正常流程（避免阻塞用户）
        continueNavigation(to, next);
      });
    });
    return; // 异步处理，直接返回
  }
  
  // 跳过检查的页面，继续正常流程
  continueNavigation(to, next);
});

export default RouterModel;
