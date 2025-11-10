<template>
  <van-action-sheet 
    v-model="visible" 
    :close-on-click-overlay="true" 
    title="线下支付方式及凭据上传"
    @close="handleClose"
  >
    <div class="offline_pay_body">
      <van-cell-group>
        <van-cell title="一、支付通道：" class="pay-step"/>
        <van-cell v-for="(channel, index) in paymentChannels"
          :key="index"
          clickable
          @click="selectedChannelIndex = index">
          <template #title>
            <div class="channel-container">
                <div class="bank-type">{{ index + 1 }}. {{ channel.bankType }}</div>
                <ul>
                <li><span>开户行：</span>{{ channel.bankName }}</li>
                <li><span>户名：</span>{{ channel.accountName }}</li>
                <li><span>账号：</span>{{ channel.accountNumber }}</li>
                </ul>
            </div>
          </template>
          <!-- <van-radio :name="index" :checked="selectedChannelIndex === index"/> -->
        </van-cell>
        <van-cell title="二、实付金额" class="pay-step">
          <span class="red">{{ actualPrice | yuan }}</span>
        </van-cell>
        <van-cell title="三、上传支付凭证" class="pay-step">
          <van-uploader :afterRead="uploadPayVoucher">
            <div class="upload_container">
              <img
                :src="payVoucherUrl + '?x-oss-process=image/resize,m_fill,h_50,w_50'" 
                width="50" 
                height="50" 
                alt="你的支付凭证" 
                v-if="payVoucherUrl"
              >
              <van-icon name="camera_full" v-else></van-icon>
            </div>
          </van-uploader>
        </van-cell>
      </van-cell-group>
    </div>
    <van-button 
      class="pay_submit" 
      @click="handleButtonClick" 
      type="primary" 
      bottomAction
    >
      {{ buttonText }}
    </van-button>
  </van-action-sheet>
</template>

<script>
import { ActionSheet, Cell, CellGroup, Button, Uploader, Icon, Radio } from 'vant';

export default {
  name: 'OfflinePaymentModal',
  
  components: {
    [ActionSheet.name]: ActionSheet,
    [Cell.name]: Cell,
    [CellGroup.name]: CellGroup,
    [Button.name]: Button,
    [Uploader.name]: Uploader,
    [Icon.name]: Icon,
    [Radio.name]: Radio
  },

  props: {
    // 控制弹窗显示/隐藏
    value: {
      type: Boolean,
      default: false
    },
    // 实付金额
    actualPrice: {
      type: Number,
      required: true
    },
    /**
     * 支付通道列表
     * 每个元素应包含:
     * - bankName: 银行名称
     * - accountName: 开户名
     * - accountNumber: 账号
     * - branch: 具体开户行
     */
    paymentChannels: {
        type: Array,
        default: () => []
    },
    // 按钮文字
    buttonText: {
      type: String,
      default: '去支付'
    },
    // 按钮点击事件处理函数
    onButtonClick: {
      type: Function,
      default: () => {}
    },
    // 文件上传处理函数
    onUpload: {
      type: Function,
      default: () => {}
    }
  },

  data() {
    return {
      payVoucherUrl: '',
      selectedChannelIndex: 0 // 默认选择第一个支付通道
    };
  },

  computed: {
    visible: {
      get() {
        return this.value;
      },
      set(val) {
        this.$emit('input', val);
      }
    }
  },

  methods: {
    handleClose() {
      this.$emit('close');
    },

    handleButtonClick() {
      const selectedChannel = this.paymentChannels[this.selectedChannelIndex];
      // 调用传入的点击事件处理函数，并传递支付凭证URL
      this.onButtonClick({
        payVoucherUrl: this.payVoucherUrl,
        paymentChannel: selectedChannel
    });
    },

    uploadPayVoucher(file) {
      // 调用传入的上传处理函数
      this.onUpload(file, (url) => {
        this.payVoucherUrl = url;
      });
    }
  }
};
</script>

<style lang="scss" scoped>
.offline_pay_body {
  padding-left: 20px;
  padding-right: 20px;
  padding-bottom: 50px;
}

.offline_pay_body img {
  padding-left: 20px;
}

.offline_pay_body ul {
  padding-left: 20px;
}

.offline_pay_body li span {
//   font-weight: bold;
}

.upload_container {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 50px;
  height: 50px;
  border: 1px dashed #ccc;
  border-radius: 4px;
}

.pay_submit {
  position: fixed;
  bottom: 0;
  width: 100%;
}

.channel-container {
    margin-left: 20px;
}

.channel-container .bank-type {
    font-weight: bold;
    margin-bottom: 5px;
}

.offline_pay_body .van-cell {
    padding-top: 5px;
}

.offline_pay_body .pay-step .van-cell__title {
    font-weight: bold;
    font-size: 110%;
}
</style>
