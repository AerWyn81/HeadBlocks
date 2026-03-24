# Translations

All plugin messages (except console messages) can be translated.

To add a translation:

1. Duplicate the `messages_en.yml` file in the language folder
2. Rename it keeping the `messages_xx.yml` format
3. Translate the messages
4. In [config.yml](global-settings.md), set the `xx` code in the `language` section

## Color Support

HeadBlocks supports multiple color formats thanks to [IridiumColorAPI](https://github.com/Iridium-Development/IridiumColorAPI):

**Minecraft native colors:**

```
&6&lH&e&lead&6&lB&e&llocks
```

**Hexadecimal colors:**

```
<SOLID:FFFF00>HeadBlocks
```

**Gradient:**

```
<GRADIENT:ff0000>HeadBlocks</GRADIENT:ffff00>
```

**Rainbow:**

```
<RAINBOW1>HeadBlocks</RAINBOW>
```

**Gradient saturation:**

```
<RAINBOW100>HeadBlocks</RAINBOW>
```
