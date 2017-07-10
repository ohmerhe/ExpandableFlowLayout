
## ExpandableFlowLayout

> 这个库是在 [FlowLayout](https://github.com/nex3z/FlowLayout) 基础上修改的，增加了可扩展功能，所以这个库的所有功能都支持。

[ ![Download](https://api.bintray.com/packages/ohmerhe/maven/ExpanableFlowLayout/images/download.svg) ](https://bintray.com/ohmerhe/maven/ExpanableFlowLayout/_latestVersion)

## 依赖

```
repositories {
    jcenter()
}

compile 'com.ohmerhe:expanableflowlayout:0.9.1'
```

## 效果图

![](http://7xpox6.com1.z0.glb.clouddn.com/expandableflowlayout.gif)

## 使用

### xml 

```
<com.ohmerhe.flowlayout.ExpandableFlowLayout
        android:id="@+id/flowLayout2"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="32dp"
        android:background="#f8f8f8"
        app:childSpacing="auto"
        app:childSpacingForLastRow="align"
        app:rowSpacing="8dp">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:gravity="center"
            android:padding="8dp"
            android:background="@drawable/bg_tag"
            android:textColor="@color/tag_color"
            android:text="@string/tag0"/>

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:gravity="center"
            android:padding="8dp"
            android:background="@drawable/bg_tag"
            android:textColor="@color/tag_color"
            android:text="@string/tag1"/>
    </com.ohmerhe.flowlayout.ExpandableFlowLayout>
```

### 代码中

直接添加子视图

```
expandableFlowLayout.addView(subView)
```

## 属性

| 属性              | 格式                       | 描述                                                                                                                                          |
|------------------------|------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------|
| flow                   | boolean                      | `true` 允许使用 flow. `false` 将所有的子视图限制在一行. 默认值是 `true`.                                                         |
| childSpacing           | `auto`/dimension             | 子视图水平间距. 支持 `auto` 自动模式，或者是指定的大小。默认是 0dp。                                                     |
| childSpacingForLastRow | `auto`/`align`/<br>dimension | 最后一行子视图的水平解决. 支持 `auto`、 `align` 或者一个指定大小. 如果不设置的话, 会使用 `childSpacing` 的值。 |
| rowSpacing             | `auto`/dimension             | 行间距。支持 `auto` 自动模式，或者是指定的大小。默认是 0dp。                                                            |
| hGravity               | enum                      | `center` 视图从右至左布局，`left` 从左至右布局，`center` 居中显示. 默认值是 `left`.                                       |
| maxRows                | integer                      | 显示行数的最大值，当视图行数超过该值时不显示，或者展开后显示。 默认值是 `Integer.MAX_VALUE`.                                       |
| supportExpand           | boolean                      | 当视图需要显示行数大于 `maxRows` 时，根据 supportExpand 判断是否通过可展开的方式显示视图，`true` 代表会显示可展开箭头，点击可展开或收起多出的子视图，`false` 代表视图只显示 `maxRows` 限制内行数的视图                           |

`auto` 以为着间距是根据 `ExpandableFlowLayout` 视图的大小和每行子视图的数量（或者行数）计算出来的，此时每行的子视图（或者行与行之间）是均匀分布的。

`childSpacingForLastRow` 的 `align` 代表最后一行子视图的水平间距和上一行保持一致。如果这个时候只有一行视图，被赋予 `align` 值的 `childSpacingForLastRow` 属性会被忽略，而直接使用 `childSpacing` 的值计算。

## Licence

```
Copyright 2017 ohmer

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```