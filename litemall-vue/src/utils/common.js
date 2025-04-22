//为了解决此版本vant的Address图标无法显示的问题。from: https://blog.csdn.net/weixin_44582045/article/details/124662065
export function loadAddressIcons(dataList) {
    
    setTimeout(() => {
    
        // 添加图标到编辑按钮上
        const editIconEls = document.querySelectorAll('.van-icon-edit')
        editIconEls.forEach(editIconEl => {
        editIconEl.classList.remove('van-icon', 'van-icon-edit')
        editIconEl.classList.add('fas', 'fa-pen')    //添加图标的方法来自：https://blog.csdn.net/weixin_59367964/article/details/130368735

        const tagEl = document.querySelectorAll('.van-address-item__tag');
        if (tagEl.length > 0) {
            return;
        }

        // 添加默认标签
        let index = 0;
        const hasDefault = false;
        for (index = 0; index < dataList.length; index++) {
            const item = dataList[index];
            console.log('item', item)
            if (item.isDefault) {
                hasDefault = true;
                break;
            }
        }
        if (hasDefault) {
            const nameEl = document.querySelectorAll('.van-address-item__name')[index]
            const defaultTagEl = document.createElement('span');
            defaultTagEl.classList.add('van-tag', 'van-tag--round', 'van-tag--danger', 'van-address-item__tag')
            defaultTagEl.innerText = '默认';
            nameEl.appendChild(defaultTagEl);
        }

    })
    }, 500);
}