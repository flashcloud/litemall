const Tabbar = () => import('@/components/Tabbar/');

export default [
  {
    path: '/',
    name: 'shop',
    components: {
      default: () => import('@/views/shop/tabbar-shop'),
      tabbar: Tabbar
    },
    meta: {
      keepAlive: true,
      showHeader:false
    },
  },
  {
    path: '*',
    redirect: {
      name: 'shop'
    }
  }
];
