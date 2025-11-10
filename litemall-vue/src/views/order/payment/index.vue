<template>
  <div class="payment">
    <div class="time_down payment_group">
      请在
      <span class="red">半小时内</span>
      完成付款，否则系统自动取消订单
    </div>

    <van-cell-group class="payment_group">
      <van-cell title="订单编号" :value="order.orderInfo.orderSn"/>
      <van-cell title="实付金额">
        <span class="red">{{order.orderInfo.actualPrice *100 | yuan}}</span>
      </van-cell>
    </van-cell-group>

    <div class="pay_way_group">
      <div class="pay_way_title">选择支付方式</div>
      <van-radio-group v-model="payWay">
        <van-cell-group>
          <!-- <van-cell>
            <template slot="title">
              <img src="../../../assets/images/ali_pay.png" alt="支付宝" width="82" height="29">
            </template>
            <van-radio name="ali"/>
          </van-cell> -->
          <van-cell>
            <template slot="title">
              <img src="../../../assets/images/wx_pay.png" alt="微信支付" width="113" height="23">
            </template>            
            <van-radio name="wx"/>
          </van-cell>
          <van-cell>
            <template slot="title">
                <svg width="130" height="29" viewBox="0 0 130 29" fill="none" xmlns="http://www.w3.org/2000/svg">
                <!-- 银联卡片图标 -->
                <rect x="2" y="5" width="20" height="14" rx="2" fill="white" stroke="#1A3E72" stroke-width="1.5"/>
                <rect x="6" y="9" width="12" height="6" fill="#1A3E72"/>
                <rect x="6" y="11" width="12" height="2" fill="#FFD700"/>
                <text x="35" y="19" font-family="Arial" font-size="17" fill="#555">线下支付</text>
                </svg>
            </template>
            <van-radio name="offline" @click="clickOfflinePay"/>
          </van-cell>
        </van-cell-group>
      </van-radio-group>
    </div>

    <van-button class="pay_submit" @click="pay" type="primary" bottomAction>去支付</van-button>
    <offline-payment-modal
      v-model="offlinePayVisible"
      :actual-price="order.orderInfo.actualPrice * 100"
      :payment-channels="order.paymentChannels"
      :button-text="'确认支付'"
      :on-button-click="pay"
      :on-upload="handleUploadVoucher"
      @close="handleOfflineModalClose"
    />
  </div>
</template>

<script>
import { Radio, RadioGroup, Dialog, Uploader, ActionSheet } from 'vant';
import { orderDetail, orderPrepay, orderH5pay, storageUpload } from '@/api/api';
import _ from 'lodash';
import { getLocalStorage, setLocalStorage } from '@/utils/local-storage';
import OfflinePaymentModal from '@/components/offline-pay/index.vue';

export default {
  name: 'payment',

  data() {
    return {
      payWay: 'wx',
      order: {
        orderInfo: {},
        orderGoods: []
      },
      orderId: 0,
      offlinePayVisible: false,
      payVoucherUrl: ''
    };
  },
  created() {
    if (_.has(this.$route.params, 'orderId')) {
      this.orderId = this.$route.params.orderId;
      this.getOrder(this.orderId);
    }
  },
  methods: {
    clickOfflinePay() {
      this.offlinePayVisible = true;
    },
    handleUploadVoucher(file, callback) {
      storageUpload(file.file).then(res => {
        if (res.data.errno === 0) {
          this.payVoucherUrl = res.data.data.url;
          callback(this.payVoucherUrl);
          this.payWay = 'offline';
        } else {
            this.payVoucherUrl='';
            callback(this.payVoucherUrl);
            Dialog.alert({ message: res.data.errmsg });
        }
      }).catch(err => {
          this.payVoucherUrl='';
          callback(this.payVoucherUrl);
          Dialog.alert({ message: '上传支付凭据失败，请稍后再试' });
      });
    },
    handleOfflineModalClose() {
      if (this.payWay === 'offline' && this.payVoucherUrl === '') {
        this.payWay = 'wx';
      }
    },
    getOrder(orderId) {
      orderDetail({orderId: orderId}).then(res => {
        this.order = res.data.data;
      });
    },
    pay() {
        if (this.payWay === 'offline' && this.payVoucherUrl === '') {
          Dialog.alert({ message: '请成功办理线下支付后，点击上传付款凭据，再继续' });
          return;
        }

        if (this.payWay === 'offline') {
            let that = this;
            orderPrepay({ orderId: this.orderId, payType: this.payWay, payVoucher: this.payVoucherUrl })
                .then(res => {
                Dialog.alert({ message: '支付凭证上传成功，请等待审核' });
                that.$router.replace({
                    name: 'paymentStatus',
                    params: {
                    status: 'success'
                    }
                });
                })
                .catch(err => {
                    Dialog.alert({ message: err.data.errmsg });
                    return;
                });
            return;
         }

         this.payVoucherUrl = '';
      
        if (this.payWay === 'wx') {
          let ua = navigator.userAgent.toLowerCase();
          let isWeixin = ua.indexOf('micromessenger') != -1;
          if (isWeixin) {
            orderPrepay({ orderId: this.orderId, payType: this.payWay})
              .then(res => {
                let data = res.data.data;
                let prepay_data = JSON.stringify({
                  appId: data.appId,
                  timeStamp: data.timeStamp,
                  nonceStr: data.nonceStr,
                  package: data.packageValue,
                  signType: 'MD5',
                  paySign: data.paySign
                });
                setLocalStorage({ prepay_data: prepay_data });

                if (typeof WeixinJSBridge == 'undefined') {
                  if (document.addEventListener) {
                    document.addEventListener(
                      'WeixinJSBridgeReady',
                      this.onBridgeReady,
                      false
                    );
                  } else if (document.attachEvent) {
                    document.attachEvent(
                      'WeixinJSBridgeReady',
                      this.onBridgeReady
                    );
                    document.attachEvent(
                      'onWeixinJSBridgeReady',
                      this.onBridgeReady
                    );
                  }
                } else {
                  this.onBridgeReady();
                }
              })
              .catch(err => {
                Dialog.alert({ message: err.data.errmsg });
                that.$router.replace({
                  name: 'paymentStatus',
                  params: {
                    status: 'failed'
                  }
                });
              });
          } else {
            orderH5pay({ orderId: this.orderId })
              .then(res => {
                let data = res.data.data;
                window.location.replace(
                  data.mwebUrl +
                  '&redirect_url=' +
                  encodeURIComponent(
                    window.location.origin +
                    '/#/?orderId=' +
                    this.orderId +
                    '&tip=yes'
                  )
                );
              })
              .catch(err => {
                Dialog.alert({ message: err.data.errmsg });
              });
          }
        } else {
          //todo : alipay
        }
    },
    onBridgeReady() {
      let that = this;
      let data = getLocalStorage('prepay_data');
      // eslint-disable-next-line no-undef
      WeixinJSBridge.invoke(
        'getBrandWCPayRequest',
        JSON.parse(data.prepay_data),
        function(res) {
          if (res.err_msg == 'get_brand_wcpay_request:ok') {
            that.$router.replace({
              name: 'paymentStatus',
              params: {
                status: 'success'
              }
            });
          } else if (res.err_msg == 'get_brand_wcpay_request:cancel') {
            that.$router.replace({
              name: 'paymentStatus',
              params: {
                status: 'cancel'
              }
            });
          } else {
            that.$router.replace({
              name: 'paymentStatus',
              params: {
                status: 'failed'
              }
            });
          }
        }
      );
    }
  },

  components: {
    [Radio.name]: Radio,
    [RadioGroup.name]: RadioGroup,
    [Dialog.name]: Dialog,
    [Uploader.name]: Uploader,
    [ActionSheet.name]: ActionSheet,
    OfflinePaymentModal
  }
};
</script>

<style lang="scss" scoped>
.payment_group {
  margin-bottom: 10px;
}

.time_down {
  background-color: #fffeec;
  padding: 10px 15px;
}

.pay_submit {
  position: fixed;
  bottom: 0;
  width: 100%;
}

.pay_way_group img {
  vertical-align: middle;
}

.pay_way_title {
  padding: 15px;
  background-color: #fff;
}

.offline_pay_body {
    padding-left: 20px;
    padding-right: 20px;
    padding-bottom: 50px;
}
.offline_pay_body ul {
    padding-left: 40px;
}
.offline_pay_body li span {
    font-weight: bold;
}
</style>
