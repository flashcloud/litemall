var util = require('../../../utils/util.js');
var api = require('../../../config/api.js');
var app = getApp();

// 这个地方是要写全路径

Page({
  data: {
    traderList: [],
    total: 0
  },
  onLoad: function(options) {
    // 页面初始化 options为页面跳转所带来的参数
  },
  onReady: function() {
    // 页面渲染完成
  },
  onShow: function() {
    // 页面显示
    this.getTraderList();
  },
  getTraderList() {
    let that = this;
    util.request(api.TraderList).then(function(res) {
      if (res.errno === 0) {
        that.setData({
          traderList: res.data.list,
          total: res.data.total
        });
      }
    });
  },
  traderAddOrUpdate(event) {
    console.log(event)

    //返回之前，先取出上一页对象，并设置traderId
    var pages = getCurrentPages();
    var prevPage = pages[pages.length - 2];

    if (prevPage.route == "pages/checkout/checkout") {
      try {
        wx.setStorageSync('traderId', event.currentTarget.dataset.traderId);
      } catch (e) {

      }

      let traderId = event.currentTarget.dataset.traderId;
      if (traderId && traderId != 0) {
        wx.navigateBack();
      } else {
        wx.navigateTo({
          url: '/pages/ucenter/traderAdd/traderAdd?id=' + traderId
        })
      }

    } else {
      wx.navigateTo({
        url: '/pages/ucenter/traderAdd/traderAdd?id=' + event.currentTarget.dataset.traderId
      })
    }
  },
  deleteTrader(event) {
    console.log(event.target)
    let that = this;
    wx.showModal({
      title: '',
      content: '确定要删除公司吗？',
      success: function(res) {
        if (res.confirm) {
          let traderId = event.target.dataset.traderId;
          util.request(api.TraderDelete, {
            id: traderId
          }, 'POST').then(function(res) {
            if (res.errno === 0) {
              that.getTraderList();
              wx.removeStorage({
                key: 'traderId',
                success: function(res) {},
              })
            } else {
              // util.showErrorToast(res.errmsg)
              wx.showModal({
                title: '禁止删除',
                content: res.errmsg,
                confirmColor: '#ff0000',
                showCancel: false,
                complete: (res) => {
                  if (res.cancel) {
                    
                  }
              
                  if (res.confirm) {
                    
                  }
                }
              })
            }
          });
          console.log('用户点击确定')
        }
      }
    })
    return false;

  },
  onHide: function() {
    // 页面隐藏
  },
  onUnload: function() {
    // 页面关闭
  }
})