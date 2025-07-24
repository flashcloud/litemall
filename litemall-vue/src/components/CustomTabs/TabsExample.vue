<template>
  <div class="tabs-example">
    <!-- 基础用法 -->
    <div class="section">
      <h3>基础用法</h3>
      <custom-tabs v-model="basicTab" :tabs="basicTabs" @change="onBasicTabChange">
        <template v-slot="{ currentTab }">
          <div class="content-area">
            <p>当前显示第 {{ currentTab + 1 }} 个标签页的内容</p>
          </div>
        </template>
      </custom-tabs>
    </div>

    <!-- 带徽章的标签 -->
    <div class="section">
      <h3>带徽章标签</h3>
      <custom-tabs v-model="badgeTab" :tabs="badgeTabs">
        <template v-slot="{ activeTabData }">
          <div class="content-area">
            <h4>{{ activeTabData.title }}</h4>
            <p>{{ activeTabData.description }}</p>
          </div>
        </template>
      </custom-tabs>
    </div>

    <!-- 不同主题风格 -->
    <div class="section">
      <h3>绿钻主题</h3>
      <custom-tabs 
        v-model="themeTab" 
        :tabs="themeTabs" 
        class="green-diamond-theme"
      >
        <template v-slot="{ activeTabData }">
          <div class="content-area">
            <p>{{ activeTabData.description }}</p>
          </div>
        </template>
      </custom-tabs>
    </div>

    <div class="section">
      <h3>情侣主题</h3>
      <custom-tabs 
        v-model="loverTab" 
        :tabs="loverTabs" 
        class="lover-theme"
      >
        <template v-slot="{ activeTabData }">
          <div class="content-area">
            <p>{{ activeTabData.description }}</p>
          </div>
        </template>
      </custom-tabs>
    </div>

    <div class="section">
      <h3>听书主题</h3>
      <custom-tabs 
        v-model="audioTab" 
        :tabs="audioTabs" 
        class="audio-theme"
      >
        <template v-slot="{ activeTabData }">
          <div class="content-area">
            <p>{{ activeTabData.description }}</p>
          </div>
        </template>
      </custom-tabs>
    </div>
  </div>
</template>

<script>
import CustomTabs from '@/components/CustomTabs/index.vue';

export default {
  name: 'TabsExample',
  components: {
    CustomTabs
  },
  data() {
    return {
      basicTab: 0,
      badgeTab: 0,
      themeTab: 0,
      loverTab: 0,
      audioTab: 0,
      
      // 基础标签数据
      basicTabs: [
        { title: '标签一' },
        { title: '标签二' },
        { title: '标签三' }
      ],
      
      // 带徽章的标签数据
      badgeTabs: [
        { 
          title: '超级会员', 
          subtitle: '限时福利',
          badge: 'HOT',
          description: '享受超级会员专属权益和服务'
        },
        { 
          title: '普通会员', 
          subtitle: '基础服务',
          description: '基础会员服务，满足日常需求'
        },
        { 
          title: '年度会员', 
          subtitle: '超值优惠',
          badge: 'NEW',
          description: '年度会员超值优惠，一次购买全年享受'
        }
      ],
      
      // 绿钻主题标签
      themeTabs: [
        { 
          title: '豪华绿钻', 
          subtitle: '顶级体验',
          description: '豪华绿钻会员，享受最顶级的服务体验'
        },
        { 
          title: '普通绿钻', 
          subtitle: '基础权益',
          description: '普通绿钻会员，基础权益满足日常使用'
        }
      ],
      
      // 情侣主题标签
      loverTabs: [
        { 
          title: '情侣会员', 
          subtitle: '双人专享',
          description: '专为情侣打造的会员服务，双人共享甜蜜时光'
        },
        { 
          title: '单身会员', 
          subtitle: '个人专属',
          description: '单身用户专属会员，享受个人定制服务'
        }
      ],
      
      // 听书主题标签
      audioTabs: [
        { 
          title: '听书VIP', 
          subtitle: '无限畅听',
          description: '听书VIP会员，海量有声内容无限畅听'
        },
        { 
          title: '免费用户', 
          subtitle: '基础体验',
          description: '免费用户基础体验，部分内容限制收听'
        }
      ]
    };
  },
  methods: {
    onBasicTabChange(tabIndex, tabData) {
      console.log('基础标签切换:', tabIndex, tabData);
    }
  }
};
</script>

<style lang="scss" scoped>
.tabs-example {
  padding: 20px;
  background: #f5f5f5;
  min-height: 100vh;
  
  .section {
    margin-bottom: 40px;
    
    h3 {
      margin-bottom: 16px;
      color: #333;
      font-size: 18px;
      font-weight: 600;
    }
    
    .content-area {
      padding: 20px;
      background: #fff;
      border-radius: 8px;
      margin-top: 16px;
      
      h4 {
        margin-bottom: 12px;
        color: #333;
        font-size: 16px;
      }
      
      p {
        color: #666;
        line-height: 1.5;
        margin: 0;
      }
    }
  }
}
</style>
