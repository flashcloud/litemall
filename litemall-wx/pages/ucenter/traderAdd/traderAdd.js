var util = require('../../../utils/util.js');
var api = require('../../../config/api.js');
var check = require('../../../utils/check.js');
var area = require('../../../utils/area.js');

var app = getApp();
Page({
  data: {
    trader: {
      id: 0,
      companyName: '',
      nickname: '',
      phoneNum: '',
      address: '',
      isDefault: 0
    },
    traderId: 0
  },
  bindinputTaxid(event) {
    let trader = this.data.trader;
    trader.taxid = event.detail.value;
    this.setData({
      trader: trader
    });
  },
  bindinputCompanyName(event) {
    let trader = this.data.trader;
    trader.companyName = event.detail.value;
    this.setData({
      trader: trader
    });
  },
  bindinputNickname(event) {
    let trader = this.data.trader;
    trader.nickname = event.detail.value;
    this.setData({
      trader: trader
    });
  },  
  bindinputPhoneNum(event) {
    let trader = this.data.trader;
    trader.phoneNum = event.detail.value;
    this.setData({
      trader: trader
    });
  },
  bindinputAddress(event) {
    let trader = this.data.trader;
    trader.address = event.detail.value;
    this.setData({
      trader: trader
    });
  },
  bindIsDefault() {
    let trader = this.data.trader;
    //TODO: trader.isDefault = !trader.isDefault;
    trader.isDefault = !true;
    this.setData({
      trader: trader
    });
  },
  getAddressDetail() {
    let that = this;
    
    util.request(api.TraderDetail,  {
      id: that.data.traderId
    }).then(function(res) {
      if (res.errno === 0) {
        if (res.data) {
          that.setData({
            trader: res.data
          });
        }
      }
    });
  },
  onLoad: function(options) {
    // 页面初始化 options为页面跳转所带来的参数
    console.log(options)
    if (options.id && options.id != 0) {
      this.setData({
        traderId: options.id
      });
      this.getAddressDetail();
    }
  },
  onReady: function() {

  },
  cancelTrader() {
    wx.navigateBack();
  },
  saveTrader() {
    console.log(this.data.trader)
    let trader = this.data.trader;

    if (trader.taxid == '') {
      util.showErrorToast('请输入税号');
      return false;
    }       
    if (trader.companyName == '') {
      util.showErrorToast('请输入公司名称');
      return false;
    }
    if (trader.nickname == '') {
      util.showErrorToast('请输入公司简称');
      return false;
    }       

    let that = this;
    util.request(api.TraderSave, {
      id: trader.id,
      companyName: trader.companyName,
      nickname: trader.nickname,
      taxid: trader.taxid,
      phoneNum: trader.phoneNum,
      address: trader.address
      // isDefault: address.isDefault
    }, 'POST').then(function(res) {
      if (res.errno === 0) {
        //返回之前，先取出上一页对象，并设置addressId
        var pages = getCurrentPages();
        var prevPage = pages[pages.length - 2];
        console.log(prevPage);
        if (prevPage.route == "pages/checkout/checkout") {
          prevPage.setData({
            traderId: res.data
          })

          try {
            wx.setStorageSync('traderId', res.data);
          } catch (e) {

          }
          console.log("set trader");
        }
        wx.navigateBack();
      } else {
        util.showErrorToast(res.errmsg);
      }
    });

  },
  onShow: function() {
    // 页面显示
  },
  onHide: function() {
    // 页面隐藏

  },
  onUnload: function() {
    // 页面关闭

  }
})