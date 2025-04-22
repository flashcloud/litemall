
<template>
    <div>
      <van-address-list v-model="chosenAddressId" :list="traderList" @add="onAdd" @edit="onEdit" @select="onSelect" :add-button-text="addBtnText" :default-tag-text="defaultText"/>
    </div>
 
  </template>
  
  <script>
  import { traderList, traderDetail, traderSave, traderDelete } from '@/api/api';
  import { loadAddressIcons } from '@/utils/common';
  import { AddressList, NavBar } from 'vant';
  import { setLocalStorage } from '@/utils/local-storage';
  
  export default {
    data() {
      return {
        chosenAddressId: -1,
        addBtnText: '新增我的公司',
        defaultText: '默认',
        traderList: []
      };
    },
  
    mounted() {
        //loadAddressIcons(this.traderList);  
    },

    created() {
      this.loadTrader();
    },
    methods: {
      onAdd() {
        this.$router.push({ name: 'trader-edit', query: { traderId: -1 } });
      },
      onEdit(item, index) {
        this.$router.push({ name: 'trader-edit', query: { traderId: item.id } });
      },
      onSelect(item, index) {
        setLocalStorage({ TraderId: item.id });
        this.$router.go(-1);
      },         
      goback() {
        this.$router.go(-1);
      },
      loadTrader() {
        traderList().then(res => {
          var list = res.data.data.list;
          for(var i = 0; i < list.length; i++ ){
            var item = list[i]
            this.traderList.push({
              id: item.id,
              name: item.companyName,
              tel: item.phoneNum,
              address: item.address,
              isDefault: item.isDefault
            })
          }
          loadAddressIcons(this.traderList);
        })
      }
    },
  
    components: {
      [NavBar.name]: NavBar,
      [AddressList.name]: AddressList 
    }
  };
  </script>
  
  
  <style lang="scss" scoped>
    .addressGroup {
        margin-bottom: 10px;
        &:last-child {
        margin-bottom: 0;
        }
    }

    .van-address-item__edit {
        font-size: 12px;
        padding-left: 10px;
    }
    
    .bottom_btn {
        position: fixed;
        bottom: 0;
    }
  </style>
  