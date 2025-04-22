<template>
    <div>
        <van-nav-bar title="编辑我的公司"  @click-left="goback" ref="topNavbar"/>
        <van-form @submit="onSave" class="edit-form">
            <van-cell-group inset>
                <van-field
                v-model="traderInfo.taxid"
                name="taxid"
                label="税    号"
                placeholder="税号"
                :rules="[{ required: true, message: '税号不能为空' }]"/>
                <van-field
                v-model="traderInfo.companyName"
                name="companyName"
                label="公司名称"
                placeholder="公司名称"
                :rules="[{ required: true, message: '公司名称不能为空' }]"/>
                <van-field
                v-model="traderInfo.nickname"
                name="nickname"
                label="公司简称"
                placeholder="公司简称"
                :rules="[{ required: true, message: '公司简称不能为空' }]"/>
            </van-cell-group>
            <van-cell-group inset>
                <van-field
                v-model="traderInfo.phoneNum"
                name="phoneNum"
                label="联系电话"
                placeholder="联系电话"
                :rules="[{ required: false }]"/>
                <van-field
                v-model="traderInfo.address"
                name="address"
                label="注册地址"
                placeholder="注册地址"
                :rules="[{ required: false }]"/>
            </van-cell-group>
            <van-cell-group inset>
                <van-field name="isDefault" label="是否默认">
                    <template #input>
                        <van-switch v-model="traderInfo.isDefault"/>
                    </template>
                </van-field>
            </van-cell-group>
            <div style="margin: 16px; padding-bottom: 40px;" >
                <van-divider />
                <van-button round block type="danger" native-type="submit" @click="onSave" >保存</van-button>
                <van-button round block type="default" native-type="button" @click="onDelete">删除</van-button>
            </div>
        </van-form>
    </div>
</template>

<script>
import { Form, Field, CellGroup, Switch, Divider, NavBar } from 'vant';
import { traderDetail, traderSave, traderDelete } from '@/api/api';
import { removeLocalStorage } from '@/utils/local-storage';

export default {
  name: 'trader-edit',

  data() {
    return {
      traderId: 0,
      traderInfo: {}
    };
  },
  created() {
    this.traderId = this.$route.query.traderId;
    if (this.traderId !== -1 && this.traderId !== 0) {
      this.init();
    }
  },

  methods: {
    init() {
        traderDetail({id: this.traderId}).then(res => {
            this.traderInfo = res.data.data
            // this.$refs.topNavbar.title = '编辑我的公司'
      });
    },
    onSave(content) {
        content.id = this.traderId == -1 ? 0 : this.traderId
        traderSave(content).then(res => {
        this.$toast('更新成功');
        this.$router.go(-1);
      }).catch(err => {
        this.$toast.fail('注意：' + err.data.errmsg);
        this.$router.go(-1);
      });
    },
    onDelete(content) {
      content.id = this.traderId == -1 ? 0 : this.traderId
      traderDelete({ id: content.id }).then(res => {
        this.$toast('删除成功');
        removeLocalStorage('TraderId')
        this.$router.go(-1);
      }).catch(err => {
        this.$toast.fail('删除失败。' + err.data.errmsg);
      });
    },
    goback() {
      this.$router.go(-1);
    }
  },

  components: {
    [NavBar.name]: NavBar,
    [Form.name]: Form,
    [Field.name]: Field,
    [Switch.name]: Switch,
    [Divider.name]: Divider,
    [CellGroup.name]: CellGroup
  }
};
</script>
<style lang="scss" scoped>
    .edit-form {
        margin-top: 20px;
        margin-bottom: 10px;
        background-color: #fff;
    }
    .van-cell-group {
        margin-bottom: 10px;
    }
    .van-button {
        margin-bottom: 15px;
    }
</style>
